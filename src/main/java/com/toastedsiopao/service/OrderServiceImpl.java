package com.toastedsiopao.service;

import com.fasterxml.jackson.core.type.TypeReference; // --- ADDED ---
import com.fasterxml.jackson.databind.ObjectMapper; // --- ADDED ---
import com.toastedsiopao.dto.OrderSubmitDto; // --- ADDED ---
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.OrderItem; // --- ADDED ---
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.OrderRepository;
import com.toastedsiopao.repository.ProductRepository; // --- ADDED ---
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
import java.sql.Date;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList; // --- ADDED ---
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
	
	// --- ADDED ---
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductService productService;

	@Autowired
	private ObjectMapper objectMapper;
	// --- END ADDED ---

	@Autowired
	private Clock clock; 
	
	// --- ADDED: DTO for parsing cart JSON ---
	private static class CartItemDto {
		public String name;
		public double price;
		public String image;
		public int quantity;
	}
	// --- END ADDED ---

	// --- ADDED: Implementation for createOrder ---
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

		// --- Transactional Block: Stock Validation and Deduction ---
		// We lock products, check stock, calculate total, and then deduct stock.
		
		BigDecimal calculatedTotal = BigDecimal.ZERO;
		List<OrderItem> orderItems = new ArrayList<>();
		
		for (Map.Entry<Long, CartItemDto> entry : cart.entrySet()) {
			Long productId = entry.getKey();
			CartItemDto cartItem = entry.getValue();
			int quantityToOrder = cartItem.quantity;

			if (quantityToOrder <= 0) {
				throw new IllegalArgumentException("Cart contains item with invalid quantity: " + cartItem.name);
			}

			// Find and lock the product
			Product product = productRepository.findById(productId)
					.orElseThrow(() -> new IllegalArgumentException("Product not found: " + cartItem.name));

			// Check stock
			if (product.getCurrentStock() < quantityToOrder) {
				throw new IllegalArgumentException("Insufficient stock for: " + product.getName() + 
												   ". Requested: " + quantityToOrder + ", Available: " + product.getCurrentStock());
			}
			
			// Use the product's price from DB, not the cart, for security
			BigDecimal itemPrice = product.getPrice();
			BigDecimal itemTotal = itemPrice.multiply(new BigDecimal(quantityToOrder));
			calculatedTotal = calculatedTotal.add(itemTotal);
			
			// Add to list for later saving
			orderItems.add(new OrderItem(product, quantityToOrder, itemPrice));
		}
		
		log.info("Stock validated for {} items. Total: ₱{}", orderItems.size(), calculatedTotal);
		
		// Now, create and save the order
		Order newOrder = new Order();
		newOrder.setUser(user);
		newOrder.setTotalAmount(calculatedTotal);
		newOrder.setPaymentMethod(orderDto.getPaymentMethod());
		
		// Set new statuses
		newOrder.setStatus(Order.STATUS_PENDING_VERIFICATION);
		newOrder.setPaymentStatus(Order.PAYMENT_FOR_VERIFICATION);
		
		newOrder.setPaymentReceiptImageUrl(receiptImagePath);
		newOrder.setNotes(orderDto.getNotes());

		// Set shipping details
		newOrder.setShippingFirstName(orderDto.getFirstName());
		newOrder.setShippingLastName(orderDto.getLastName());
		newOrder.setShippingPhone(orderDto.getPhone());
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
		
		// Save the order FIRST to get an ID
		Order savedOrder = orderRepository.save(newOrder);
		
		// Now, deduct stock and associate items with the saved order
		for (OrderItem item : orderItems) {
			item.setOrder(savedOrder);
			savedOrder.addItem(item); // Add to the order's list
			
			// Deduct product stock
			try {
				productService.adjustStock(item.getProduct().getId(), -item.getQuantity(), "Order #" + savedOrder.getId());
			} catch (Exception e) {
				// This should not happen if our check above was correct, but as a safeguard
				log.error("CRITICAL: Stock deduction failed AFTER validation for product ID {}. Rolling back.", item.getProduct().getId());
				throw new RuntimeException("Stock deduction failed for " + item.getProduct().getName(), e);
			}
		}

		log.info("Successfully created Order #{} for user {}", savedOrder.getId(), user.getUsername());
		return savedOrder;
	}
	// --- END ADDED ---

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