package com.toastedsiopao.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toastedsiopao.dto.OrderSubmitDto;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.OrderItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.OrderRepository;
import com.toastedsiopao.repository.ProductRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
	
	// --- ADDED ---
	@Autowired
	private EmailService emailService;
	// --- END ADDED ---
	
	private static class CartItemDto {
		public String name;
		public double price;
		public String image;
		public int quantity;
	}

	@Override
	public Order createOrder(User user, OrderSubmitDto orderDto, String receiptImagePath) {
		log.info("Attempting to create order for user: {}", user.getUsername());
		
		Map<Long, CartItemDto> cart;
		try {
			TypeReference<Map<Long, CartItemDto>> typeRef = new TypeReference<>() {};
			cart = objectMapper.readValue(orderDto.getCartDataJson(), typeRef);
			if (cart.isEmpty()) {
				throw new IllegalArgumentException("Cannot create order with an empty cart.");
			}
		} catch (Exception e) {
			log.error("Failed to parse cartDataJson for user {}: {}", user.getUsername(), e.getMessage());
			throw new RuntimeException("Error parsing cart data.", e);
		}

		BigDecimal calculatedTotal = BigDecimal.ZERO;
		List<OrderItem> orderItems = new ArrayList<>();
		
		for (Map.Entry<Long, CartItemDto> entry : cart.entrySet()) {
			Long productId = entry.getKey();
			CartItemDto cartItem = entry.getValue();
			int quantityToOrder = cartItem.quantity;

			if (quantityToOrder <= 0) {
				throw new IllegalArgumentException("Cart contains item with invalid quantity: " + cartItem.name);
			}

			Product product = productRepository.findById(productId)
					.orElseThrow(() -> new IllegalArgumentException("Product not found: " + cartItem.name));

			if (product.getCurrentStock() < quantityToOrder) {
				throw new IllegalArgumentException("Insufficient stock for: " + product.getName() + 
												   ". Requested: " + quantityToOrder + ", Available: " + product.getCurrentStock());
			}
			
			BigDecimal itemPrice = product.getPrice();
			BigDecimal itemTotal = itemPrice.multiply(new BigDecimal(quantityToOrder));
			calculatedTotal = calculatedTotal.add(itemTotal);
			
			orderItems.add(new OrderItem(product, quantityToOrder, itemPrice));
		}
		
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
			newOrder.setTransactionId(orderDto.getTransactionId()); // --- ADDED THIS LINE ---
		}
		
		newOrder.setPaymentReceiptImageUrl(receiptImagePath);
		newOrder.setNotes(orderDto.getNotes());

		newOrder.setShippingFirstName(orderDto.getFirstName());
		newOrder.setShippingLastName(orderDto.getLastName());
		newOrder.setShippingPhone(orderDto.getPhone());
		// --- THIS IS THE FIX ---
		newOrder.setShippingEmail(orderDto.getEmail());
		// --- END FIX ---
		
		String fullAddress = String.format("%s %s %s, %s, %s, %s, %s",
				Optional.ofNullable(orderDto.getHouseNo()).orElse(""),
				Optional.ofNullable(orderDto.getLotNo()).orElse(""),
				Optional.ofNullable(orderDto.getBlockNo()).orElse(""),
				orderDto.getStreet(),
				orderDto.getBarangay(),
				orderDto.getMunicipality(),
				orderDto.getProvince())
				.replaceAll("\\s+", " ").trim();
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
		return savedOrder;
	}

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
		return orderRepository.findAllByDate(null, null, pageable); 
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Order> findOrdersByStatus(String status, Pageable pageable) { 
		if (!StringUtils.hasText(status)) {
			return findAllOrders(pageable); 
		}
		return orderRepository.searchByStatusAndDate(status.toUpperCase(), null, null, pageable); 
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

		if (hasKeyword && hasStatus) {
			return orderRepository.searchOrdersByKeywordAndStatus(keyword.trim(), upperStatus, startDateTime,
					endDateTime, pageable); 
		} else if (hasKeyword) {
			return orderRepository.searchOrdersByKeyword(keyword.trim(), startDateTime, endDateTime, pageable);
		} else if (hasStatus) {
			return orderRepository.searchByStatusAndDate(upperStatus, startDateTime, endDateTime, pageable); 
		} else {
			return orderRepository.findAllByDate(startDateTime, endDateTime, pageable); 
		}
	}

	// --- NEW PRIVATE HELPER METHOD ---
	private void reverseStockForOrder(Order order, String reason) {
		log.info("Reversing stock for order #{}", order.getId());
		for (OrderItem item : order.getItems()) {
			try {
				productService.adjustStock(item.getProduct().getId(), item.getQuantity(), reason);
				log.info("Restored {} unit(s) of {} (ID: {})", 
						item.getQuantity(), item.getProduct().getName(), item.getProduct().getId());
			} catch (Exception e) {
				// Log critical error. This shouldn't fail, but if it does, we must know.
				log.error("CRITICAL: Failed to reverse stock for product ID {} during order cancellation/rejection. " +
						  "Order ID: {}, Product: {}, Qty: {}. Error: {}",
						  item.getProduct().getId(), order.getId(), item.getProduct().getName(), item.getQuantity(), e.getMessage(), e);
				// We don't re-throw, as the order status change is the primary goal.
			}
		}
	}
	
	// --- NEW IMPLEMENTATIONS ---
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
			// Payment status remains PENDING until paid on delivery
		} else { // PENDING_VERIFICATION (GCash)
			order.setStatus(Order.STATUS_PROCESSING);
			order.setPaymentStatus(Order.PAYMENT_PAID);
		}

		log.info("Order #{} accepted. Status set to PROCESSING.", orderId);
		Order savedOrder = orderRepository.save(order);
		
		// --- Send Email ---
		try {
			String subject = "Your Order has been Accepted!";
			String message = "We're happy to let you know that your order has been accepted and is now being processed. We'll send you another update once it's out for delivery.";
			emailService.sendOrderStatusUpdateEmail(savedOrder, subject, message);
		} catch (MessagingException e) {
			log.error("Failed to send 'Order Accepted' email for Order #{}", savedOrder.getId(), e);
		}
		// --- End Email ---

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
		return orderRepository.save(order);
	}
	
	@Override
	public Order shipOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));

		if (!order.getStatus().equals(Order.STATUS_PROCESSING)) {
			throw new IllegalArgumentException("Only orders in 'Processing' status can be shipped.");
		}
		
		order.setStatus(Order.STATUS_OUT_FOR_DELIVERY);
		// Payment status (especially for COD) remains unchanged until completion.
		
		log.info("Order #{} status set to OUT_FOR_DELIVERY.", orderId);
		Order savedOrder = orderRepository.save(order);

		// --- Send Email ---
		try {
			String subject = "Your Order is Out for Delivery!";
			String message = "Get ready! Your order is now with our rider and on its way to you. If you chose Cash on Delivery, please prepare the exact amount of " +
							 "₱" + savedOrder.getTotalAmount().setScale(2, RoundingMode.HALF_UP) + ".";
			emailService.sendOrderStatusUpdateEmail(savedOrder, subject, message);
		} catch (MessagingException e) {
			log.error("Failed to send 'Out for Delivery' email for Order #{}", savedOrder.getId(), e);
		}
		// --- End Email ---
		
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

		// --- Send Email ---
		try {
			String subject = "Your Order is Complete!";
			String message = "Your order has been successfully delivered and paid for. Thank you for choosing us! We hope to serve you again soon.";
			emailService.sendOrderStatusUpdateEmail(savedOrder, subject, message);
		} catch (MessagingException e) {
			log.error("Failed to send 'Order Completed' email for Order #{}", savedOrder.getId(), e);
		}
		// --- End Email ---
		
		return savedOrder;
	}
	
	// --- THIS IS THE NEWLY IMPLEMENTED METHOD ---
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
			// We allow this, but it's unusual. The "acceptOrder" step should have set GCash to PAID.
		}
		
		order.setStatus(Order.STATUS_DELIVERED);
		// Payment status is already PAID, so no change needed.
		
		log.info("Pre-Paid Order #{} status set to DELIVERED.", orderId);
		Order savedOrder = orderRepository.save(order);

		// --- Send Email ---
		try {
			String subject = "Your Order has been Delivered!";
			String message = "Your order has been successfully delivered. Thank you for choosing us! We hope to serve you again soon.";
			emailService.sendOrderStatusUpdateEmail(savedOrder, subject, message);
		} catch (MessagingException e) {
			log.error("Failed to send 'Order Delivered' email for Order #{}", savedOrder.getId(), e);
		}
		// --- End Email ---
		
		return savedOrder;
	}
	// --- END NEW IMPLEMENTATION ---
	
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
}