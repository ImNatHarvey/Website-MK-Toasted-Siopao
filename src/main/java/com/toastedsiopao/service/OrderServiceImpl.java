package com.toastedsiopao.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toastedsiopao.dto.OrderSubmitDto;
import com.toastedsiopao.model.CartItem; // --- ADDED ---
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.OrderItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.RecipeIngredient;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.OrderRepository;
import com.toastedsiopao.repository.ProductRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl; 
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList; 
import java.util.Collections; 
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductService productService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private Clock clock; 
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private NotificationService notificationService;
	
	// --- ADDED ---
	@Autowired
	private CartService cartService;
	// --- END ADDED ---
	
	private static class CartItemDto {
		public String name;
		public double price;
		public String image;
		public int quantity;
	}

	// --- MODIFICATION: This method is now rewritten ---
	@Override
	public Order createOrder(User user, OrderSubmitDto orderDto, String receiptImagePath) {
		log.info("Attempting to create order for user: {}", user.getUsername());
		
		// --- FIX: Get cart from the database via CartService ---
		List<CartItem> dbCart = cartService.getCartForUser(user);
		if (dbCart.isEmpty()) {
			throw new IllegalArgumentException("Cannot create order with an empty cart.");
		}
		// --- END FIX ---

		BigDecimal calculatedTotal = BigDecimal.ZERO;
		List<OrderItem> orderItems = new ArrayList<>();
		
		// --- FIX: Iterate over the DB cart items ---
		for (CartItem cartItem : dbCart) {
			int quantityToOrder = cartItem.getQuantity();
			Product product = cartItem.getProduct(); // Product is already loaded from CartService

			if (quantityToOrder <= 0) {
				throw new IllegalArgumentException("Cart contains item with invalid quantity: " + product.getName());
			}

			if (!"ACTIVE".equals(product.getProductStatus()) || product.getCurrentStock() < quantityToOrder) {
				throw new IllegalArgumentException("Insufficient stock for: " + product.getName() + 
												   ". Requested: " + quantityToOrder + ", Available: " + product.getCurrentStock());
			}
			
			BigDecimal itemPrice = product.getPrice();
			BigDecimal itemTotal = itemPrice.multiply(new BigDecimal(quantityToOrder));
			calculatedTotal = calculatedTotal.add(itemTotal);
			
			orderItems.add(new OrderItem(product, quantityToOrder, itemPrice));
		}
		// --- END FIX ---
		
		log.info("Stock validated for {} items. Total: ₱{}", orderItems.size(), calculatedTotal);
		
		Order newOrder = new Order();
		newOrder.setUser(user);
		newOrder.setTotalAmount(calculatedTotal);
		newOrder.setPaymentMethod(orderDto.getPaymentMethod());
		
		if ("cod".equalsIgnoreCase(orderDto.getPaymentMethod())) {
			newOrder.setStatus(Order.STATUS_PENDING);
			newOrder.setPaymentStatus(Order.PAYMENT_PENDING);
		} else {
			newOrder.setStatus(Order.STATUS_PENDING_VERIFICATION);
			newOrder.setPaymentStatus(Order.PAYMENT_FOR_VERIFICATION);
			newOrder.setTransactionId(orderDto.getTransactionId()); 
		}
		
		newOrder.setPaymentReceiptImageUrl(receiptImagePath);
		newOrder.setNotes(orderDto.getNotes());

		newOrder.setShippingFirstName(orderDto.getFirstName());
		newOrder.setShippingLastName(orderDto.getLastName());
		newOrder.setShippingPhone(orderDto.getPhone());
		newOrder.setShippingEmail(orderDto.getEmail());
		
		List<String> unitParts = new ArrayList<>();
		if (StringUtils.hasText(orderDto.getHouseNo())) {
			unitParts.add("House No. " + orderDto.getHouseNo().trim());
		}
		if (StringUtils.hasText(orderDto.getBlockNo())) {
			unitParts.add("Blk. No. " + orderDto.getBlockNo().trim());
		}
		if (StringUtils.hasText(orderDto.getLotNo())) {
			unitParts.add("Lot No. " + orderDto.getLotNo().trim());
		}
		String unitDetails = String.join(", ", unitParts);

		List<String> addressParts = new ArrayList<>();
		if (!unitDetails.isEmpty()) {
			addressParts.add(unitDetails);
		}
		if (StringUtils.hasText(orderDto.getStreet())) {
			addressParts.add(orderDto.getStreet().trim());
		}
		if (StringUtils.hasText(orderDto.getBarangay())) {
			addressParts.add(orderDto.getBarangay().trim());
		}
		if (StringUtils.hasText(orderDto.getMunicipality())) {
			addressParts.add(orderDto.getMunicipality().trim());
		}
		if (StringUtils.hasText(orderDto.getProvince())) {
			addressParts.add(orderDto.getProvince().trim());
		}

		String fullAddress = String.join(", ", addressParts);
		
		newOrder.setShippingAddress(fullAddress);
		
		Order savedOrder = orderRepository.save(newOrder);
		
		for (OrderItem item : orderItems) {
			item.setOrder(savedOrder);
			savedOrder.addItem(item);
			
			try {
				productService.adjustStock(item.getProduct().getId(), -item.getQuantity(), "Order #" + savedOrder.getId());
			} catch (Exception e) {
				log.error("CRITICAL: Stock deduction failed AFTER validation for product ID {}. Rolling back.", item.getProduct().getId());
				throw new RuntimeException("Stock deduction failed for " + item.getProduct().getName(), e);
			}
		}

		log.info("Successfully created Order #{} for user {}", savedOrder.getId(), user.getUsername());
		
		// --- ADDED: Admin Notification ---
		String notifMessage = "New " + savedOrder.getPaymentMethod().toUpperCase() + " order (#" + savedOrder.getId() + ") placed by " + user.getUsername() + ".";
		String notifLink = "/admin/orders?status=" + savedOrder.getStatus();
		notificationService.createAdminNotification(notifMessage, notifLink);
		// --- END ADDED ---

		// --- ADDED: Clear cart directly from service ---
		cartService.clearCart(user);
		log.info("Cleared cart for user {}.", user.getUsername());
		// --- END ADDED ---
		
		return savedOrder;
	}
	// --- END MODIFICATION ---

	@Override
	@Transactional(readOnly = true)
	public Optional<Order> findOrderById(Long id) {
		return orderRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Order> findOrdersByUser(User user, Pageable pageable) { 
		return orderRepository.findByUserOrderByOrderDateDesc(user, pageable); 
	}
	
	@Override
	@Transactional(readOnly = true)
	public Page<Order> findOrdersByUserAndStatus(User user, String status, Pageable pageable) {
		if (!StringUtils.hasText(status)) {
			log.debug("Fetching all orders for user: {}", user.getUsername());
			return orderRepository.findByUserOrderByOrderDateDesc(user, pageable);
		}
		log.debug("Fetching orders for user: {} with status: {}", user.getUsername(), status);
		return orderRepository.findByUserAndStatusOrderByOrderDateDesc(user, status.toUpperCase(), pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Order> findAllOrders(Pageable pageable) { 
		return orderRepository.findAll(pageable); // --- MODIFIED: Was findAllByDate(null, null, pageable) ---
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Order> findOrdersByStatus(String status, Pageable pageable) { 
		if (!StringUtils.hasText(status)) {
			return searchOrders(null, null, null, null, pageable); // --- MODIFIED: Use new search ---
		}
		return searchOrders(null, status.toUpperCase(), null, null, pageable); // --- MODIFIED: Use new search ---
	}
	
	@Override
	@Transactional(readOnly = true)
	public Page<Order> searchOrders(String keyword, String status, String startDate, String endDate,
			Pageable pageable) { 
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasStatus = StringUtils.hasText(status);
		boolean hasStartDate = StringUtils.hasText(startDate);
		boolean hasEndDate = StringUtils.hasText(endDate);

		LocalDateTime startDateTime = null;
		LocalDateTime endDateTime = null;

		try {
			if (hasStartDate) {
				startDateTime = LocalDate.parse(startDate).atStartOfDay();
			}
		} catch (Exception e) {
			log.warn("Invalid start date format: {}. Ignoring.", startDate);
		}

		try {
			if (hasEndDate) {
				endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
			}
		} catch (Exception e) {
			log.warn("Invalid end date format: {}. Ignoring.", endDate);
		}

		String upperStatus = hasStatus ? status.toUpperCase() : null;
		
		// --- START: MODIFIED 2-STEP FETCH ---
		
		Page<Long> orderIdPage;

		if (hasKeyword && hasStatus) {
			orderIdPage = orderRepository.findIdsByKeywordAndStatus(keyword.trim(), upperStatus, startDateTime,
					endDateTime, pageable); 
		} else if (hasKeyword) {
			orderIdPage = orderRepository.findIdsByKeyword(keyword.trim(), startDateTime, endDateTime, pageable);
		} else if (hasStatus) {
			orderIdPage = orderRepository.findIdsByStatusAndDate(upperStatus, startDateTime, endDateTime, pageable); 
		} else {
			orderIdPage = orderRepository.findIdsByDate(startDateTime, endDateTime, pageable); 
		}
		
		List<Long> orderIds = orderIdPage.getContent();
		
		if (orderIds.isEmpty()) {
			return new PageImpl<>(Collections.emptyList(), pageable, orderIdPage.getTotalElements());
		}
		
		List<Order> orders = orderRepository.findWithDetailsByIds(orderIds);
		
		// Re-sort the fetched orders to match the ID page's order (since IN clause doesn't guarantee order)
		Map<Long, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getId, o -> o));
		List<Order> sortedOrders = orderIds.stream().map(orderMap::get).collect(Collectors.toList());

		return new PageImpl<>(sortedOrders, pageable, orderIdPage.getTotalElements());
		// --- END: MODIFIED 2-STEP FETCH ---
	}

	private void reverseStockForOrder(Order order, String reason) {
		log.info("Reversing stock for order #{}", order.getId());
		for (OrderItem item : order.getItems()) {
			try {
				productService.adjustStock(item.getProduct().getId(), item.getQuantity(), reason);
				log.info("Restored {} unit(s) of {} (ID: {})", 
						item.getQuantity(), item.getProduct().getName(), item.getProduct().getId());
			} catch (Exception e) {
				log.error("CRITICAL: Failed to reverse stock for product ID {} during order cancellation/rejection. " +
						  "Order ID: {}, Product: {}, Qty: {}. Error: {}",
						  item.getProduct().getId(), order.getId(), item.getProduct().getName(), item.getQuantity(), e.getMessage(), e);
			}
		}
	}
	
	@Override
	public Order cancelOrder(Long orderId, User customer) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));

		if (!order.getUser().getId().equals(customer.getId())) {
			log.warn("Security violation: User {} attempted to cancel order #{} owned by user {}", 
					customer.getUsername(), orderId, order.getUser().getUsername());
			throw new IllegalArgumentException("You do not have permission to cancel this order.");
		}

		if (!order.getStatus().equals(Order.STATUS_PENDING) && !order.getStatus().equals(Order.STATUS_PENDING_VERIFICATION)) {
			throw new IllegalArgumentException("This order can no longer be cancelled as it is already " + order.getStatus());
		}

		order.setStatus(Order.STATUS_CANCELLED);
		order.setPaymentStatus(Order.PAYMENT_CANCELLED);
		
		reverseStockForOrder(order, "Order #" + order.getId() + " Cancelled by Customer");
		
		// --- ADDED: Admin Notification ---
		String notifMessage = "Customer " + customer.getUsername() + " cancelled order #" + order.getId() + ".";
		String notifLink = "/admin/orders?status=CANCELLED";
		notificationService.createAdminNotification(notifMessage, notifLink);
		// --- END ADDED ---
		
		return orderRepository.save(order);
	}

	@Override
	public Order acceptOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));

		String currentStatus = order.getStatus();
		if (!currentStatus.equals(Order.STATUS_PENDING) && !currentStatus.equals(Order.STATUS_PENDING_VERIFICATION)) {
			throw new IllegalArgumentException("Order cannot be accepted. Current status: " + currentStatus);
		}

		if (currentStatus.equals(Order.STATUS_PENDING)) { // COD
			order.setStatus(Order.STATUS_PROCESSING);
		} else { // PENDING_VERIFICATION (GCash)
			order.setStatus(Order.STATUS_PROCESSING);
			order.setPaymentStatus(Order.PAYMENT_PAID);
		}

		log.info("Order #{} accepted. Status set to PROCESSING.", orderId);
		Order savedOrder = orderRepository.save(order);
		
		String subject = "Your Order has been Accepted!";
		String message = "We're happy to let you know that your order (#" + savedOrder.getId() + ") has been accepted and is now being processed. We'll send you another update once it's out for delivery.";
		
		// --- Send Email ---
		try {
			emailService.sendOrderStatusUpdateEmail(savedOrder, subject, message);
		} catch (MessagingException e) {
			log.error("Failed to send 'Order Accepted' email for Order #{}", savedOrder.getId(), e);
		}
		// --- End Email ---
		
		// --- ADDED: User Notification ---
		notificationService.createUserNotification(savedOrder.getUser(), message, "/u/history");
		// --- END ADDED ---

		return savedOrder;
	}

	@Override
	public Order rejectOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));
		
		String currentStatus = order.getStatus();
		if (!currentStatus.equals(Order.STATUS_PENDING) && !currentStatus.equals(Order.STATUS_PENDING_VERIFICATION)) {
			throw new IllegalArgumentException("Order cannot be rejected. Current status: " + currentStatus);
		}

		order.setStatus(Order.STATUS_REJECTED);
		order.setPaymentStatus(Order.PAYMENT_REJECTED);
		
		reverseStockForOrder(order, "Order #" + order.getId() + " Rejected by Admin");

		log.info("Order #{} rejected. Status set to REJECTED.", orderId);
		Order savedOrder = orderRepository.save(order);
		
		// --- ADDED: User Notification ---
		String message = "Unfortunately, your order (#" + savedOrder.getId() + ") has been rejected. Stock has been reversed. If this was a GCash order, please contact us for a refund.";
		notificationService.createUserNotification(savedOrder.getUser(), message, "/u/history");
		// --- END ADDED ---
		
		return savedOrder;
	}
	
	@Override
	public Order shipOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));

		if (!order.getStatus().equals(Order.STATUS_PROCESSING)) {
			throw new IllegalArgumentException("Only orders in 'Processing' status can be shipped.");
		}
		
		order.setStatus(Order.STATUS_OUT_FOR_DELIVERY);
		
		log.info("Order #{} status set to OUT_FOR_DELIVERY.", orderId);
		Order savedOrder = orderRepository.save(order);

		String subject = "Your Order is Out for Delivery!";
		String message = "Get ready! Your order (#" + savedOrder.getId() + ") is now with our rider and on its way to you. If you chose Cash on Delivery, please prepare the exact amount of " +
						 "₱" + savedOrder.getTotalAmount().setScale(2, RoundingMode.HALF_UP) + ".";

		// --- Send Email ---
		try {
			emailService.sendOrderStatusUpdateEmail(savedOrder, subject, message);
		} catch (MessagingException e) {
			log.error("Failed to send 'Out for Delivery' email for Order #{}", savedOrder.getId(), e);
		}
		// --- End Email ---
		
		// --- ADDED: User Notification ---
		notificationService.createUserNotification(savedOrder.getUser(), message, "/u/history");
		// --- END ADDED ---
		
		return savedOrder;
	}

	@Override
	public Order completeCodOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));

		if (!order.getStatus().equals(Order.STATUS_OUT_FOR_DELIVERY)) {
			throw new IllegalArgumentException("Only orders 'Out for Delivery' can be marked as completed.");
		}
		
		if (!order.getPaymentMethod().equalsIgnoreCase("cod")) {
			throw new IllegalArgumentException("This action is only for 'Cash on Delivery' orders.");
		}
		
		order.setStatus(Order.STATUS_DELIVERED);
		order.setPaymentStatus(Order.PAYMENT_PAID); // Mark as paid
		
		log.info("COD Order #{} status set to DELIVERED and PAID.", orderId);
		Order savedOrder = orderRepository.save(order);

		String subject = "Your Order is Complete!";
		String message = "Your order (#" + savedOrder.getId() + ") has been successfully delivered and paid for. Thank you for choosing us! We hope to serve you again soon.";

		// --- Send Email ---
		try {
			emailService.sendOrderStatusUpdateEmail(savedOrder, subject, message);
		} catch (MessagingException e) {
			log.error("Failed to send 'Order Completed' email for Order #{}", savedOrder.getId(), e);
		}
		// --- End Email ---
		
		// --- ADDED: User Notification ---
		notificationService.createUserNotification(savedOrder.getUser(), message, "/u/history");
		// --- END ADDED ---
		
		return savedOrder;
	}
	
	@Override
	public Order completeDeliveredOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));

		if (!order.getStatus().equals(Order.STATUS_OUT_FOR_DELIVERY)) {
			throw new IllegalArgumentException("Only orders 'Out for Delivery' can be marked as delivered.");
		}
		
		if (order.getPaymentMethod().equalsIgnoreCase("cod")) {
			throw new IllegalArgumentException("This action is for pre-paid (non-COD) orders. Use 'Complete COD' action instead.");
		}
		
		if (!order.getPaymentStatus().equals(Order.PAYMENT_PAID)) {
			log.warn("Admin is completing an order (ID: {}) that is not marked as PAID. Current payment status: {}", orderId, order.getPaymentStatus());
		}
		
		order.setStatus(Order.STATUS_DELIVERED);
		
		log.info("Pre-Paid Order #{} status set to DELIVERED.", orderId);
		Order savedOrder = orderRepository.save(order);

		String subject = "Your Order has been Delivered!";
		String message = "Your order (#" + savedOrder.getId() + ") has been successfully delivered. Thank you for choosing us! We hope to serve you again soon.";

		// --- Send Email ---
		try {
			emailService.sendOrderStatusUpdateEmail(savedOrder, subject, message);
		} catch (MessagingException e) {
			log.error("Failed to send 'Order Delivered' email for Order #{}", savedOrder.getId(), e);
		}
		// --- End Email ---
		
		// --- ADDED: User Notification ---
		notificationService.createUserNotification(savedOrder.getUser(), message, "/u/history");
		// --- END ADDED ---
		
		return savedOrder;
	}
	
	// --- COGS CALCULATION HELPERS AND IMPLEMENTATION (START) ---
	
	private BigDecimal calculateOrderCogs(Order order) {
		BigDecimal totalCogs = BigDecimal.ZERO;
		
		for (OrderItem orderItem : order.getItems()) {
			Product product = orderItem.getProduct();
			int orderedQuantity = orderItem.getQuantity();

			// Accessing ingredients should not cause an N+1 if the query was set up correctly (JOIN FETCH)
			if (product.getIngredients() == null || product.getIngredients().isEmpty()) {
				log.warn("Product '{}' in order #{} has no ingredients defined. Skipping COGS.", product.getName(), order.getId());
				continue;
			}
			
			for (RecipeIngredient ingredient : product.getIngredients()) {
				BigDecimal quantityNeeded = ingredient.getQuantityNeeded();
				InventoryItem item = ingredient.getInventoryItem();
				
				if (item == null || item.getCostPerUnit() == null || quantityNeeded == null) {
					log.warn("Invalid ingredient data for product '{}'. Skipping ingredient COGS.", product.getName());
					continue;
				}
				
				// Total Cogs for this ingredient = Ordered Qty * Qty per unit * Cost per unit
				BigDecimal itemCogs = quantityNeeded
						.multiply(item.getCostPerUnit())
						.multiply(new BigDecimal(orderedQuantity));

				totalCogs = totalCogs.add(itemCogs);
			}
		}
		
		return totalCogs;
	}
	
	@Override
	@Transactional(readOnly = true)
	public BigDecimal getEstimatedCogsBetweenDates(LocalDateTime start, LocalDateTime end) {
		// This uses the custom repository query to fetch all required entities in one go
		List<Order> deliveredOrders = orderRepository.findDeliveredOrdersWithCogsDetails(start, end);
		
		BigDecimal totalCogs = BigDecimal.ZERO;
		for (Order order : deliveredOrders) {
			totalCogs = totalCogs.add(calculateOrderCogs(order));
		}
		
		return totalCogs;
	}
	
	@Override
	@Transactional(readOnly = true)
	public BigDecimal getCogsToday() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfDay = now.with(LocalTime.MIN);
		return getEstimatedCogsBetweenDates(startOfDay, now);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getCogsThisWeek() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
		return getEstimatedCogsBetweenDates(startOfWeek, now);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getCogsThisMonth() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
		return getEstimatedCogsBetweenDates(startOfMonth, now);
	}
	
	// --- COGS CALCULATION HELPERS AND IMPLEMENTATION (END) ---

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getSalesToday() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfDay = now.with(LocalTime.MIN);
		return orderRepository.findTotalSalesBetweenDates(startOfDay, now);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getSalesThisWeek() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
		return orderRepository.findTotalSalesBetweenDates(startOfWeek, now);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getSalesThisMonth() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
		return orderRepository.findTotalSalesBetweenDates(startOfMonth, now);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Long> getOrderStatusCounts() {
		return orderRepository.countOrdersByStatus().stream().collect(Collectors.toMap(row -> (String) row[0],
				row -> (Long) row[1] 
		));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getTopSellingProducts(int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		Page<Object[]> results = orderRepository.findTopSellingProducts(pageable);

		return results.getContent().stream()
				.map(row -> Map.<String, Object>of("product", (Product) row[0], "quantity", (Long) row[1]))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, BigDecimal> getSalesDataForChart(LocalDateTime start, LocalDateTime end) {
		List<Object[]> results = orderRepository.findSalesDataBetweenDates(start, end);

		return results.stream()
				.collect(Collectors.toMap(row -> ((Date) row[0]).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
						row -> (BigDecimal) row[1], 
						(oldValue, newValue) -> oldValue.add(newValue),
						LinkedHashMap::new));
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getTotalRevenueAllTime() {
		BigDecimal total = orderRepository.findTotalRevenueAllTime();
		return total != null ? total : BigDecimal.ZERO;
	}

	@Override
	@Transactional(readOnly = true)
	public long getTotalTransactionsAllTime() {
		return orderRepository.countTotalTransactionsAllTime();
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getTotalPotentialRevenue() {
		BigDecimal total = orderRepository.findTotalPotentialRevenue();
		return total != null ? total : BigDecimal.ZERO;
	}
}