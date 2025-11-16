package com.toastedsiopao.service;

import com.toastedsiopao.dto.OrderSubmitDto; // --- ADDED ---
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; 

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {

	// --- ADDED ---
	Order createOrder(User user, OrderSubmitDto orderDto, String receiptImagePath);
	// --- END ADDED ---

	Optional<Order> findOrderById(Long id);

	Page<Order> findOrdersByUser(User user, Pageable pageable); 

	Page<Order> findAllOrders(Pageable pageable); 
	
	Page<Order> findOrdersByStatus(String status, Pageable pageable); 
	
	Page<Order> searchOrders(String keyword, String status, String startDate, String endDate, Pageable pageable); // Updated

	Page<Order> findOrdersByUserAndStatus(User user, String status, Pageable pageable); // --- ADDED ---

	// --- ADDED: New methods for order management ---
	Order cancelOrder(Long orderId, User customer);
	Order acceptOrder(Long orderId);
	Order rejectOrder(Long orderId);
	
	// --- NEW METHODS FOR OUR FLOW ---
	Order shipOrder(Long orderId);
	Order completeCodOrder(Long orderId);
	Order completeDeliveredOrder(Long orderId); // --- THIS IS THE NEW METHOD ---
	// --- END NEW METHODS ---
	
	// --- END ADDED ---

	BigDecimal getSalesToday();

	BigDecimal getSalesThisWeek();

	BigDecimal getSalesThisMonth();

	Map<String, Long> getOrderStatusCounts();

	List<Map<String, Object>> getTopSellingProducts(int limit);

	Map<String, BigDecimal> getSalesDataForChart(LocalDateTime start, LocalDateTime end);

	BigDecimal getTotalRevenueAllTime();

	long getTotalTransactionsAllTime();

	BigDecimal getTotalPotentialRevenue();

	// --- START: ADDED COGS METHODS ---
	BigDecimal getEstimatedCogsBetweenDates(LocalDateTime start, LocalDateTime end);
	
	BigDecimal getCogsToday();

	BigDecimal getCogsThisWeek();

	BigDecimal getCogsThisMonth();
	// --- END: ADDED COGS METHODS ---

	// === NEW METHODS FOR REPORTING (START) ===
	/**
	 * Finds all delivered orders within a date range, fully populated with
	 * items, products, and ingredients for COGS calculation.
	 * * @param start Start time (inclusive)
	 * @param end   End time (inclusive)
	 * @return A List of fully populated Order objects.
	 */
	List<Order> findDeliveredOrdersForReport(LocalDateTime start, LocalDateTime end);

	/**
	 * Calculates the Cost of Goods Sold (COGS) for a single, given order.
	 * * @param order The Order object (must have items, products, and ingredients
	 * eagerly loaded).
	 * @return The total COGS for that order as a BigDecimal.
	 */
	BigDecimal calculateCogsForOrder(Order order);
	// === NEW METHODS FOR REPORTING (END) ===
}