package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {

	Optional<Order> findOrderById(Long id);

	Page<Order> findOrdersByUser(User user, Pageable pageable); // Updated

	Page<Order> findAllOrders(Pageable pageable); // Updated

	// --- NEW METHODS ---
	/**
	 * Finds all orders matching a specific status.
	 *
	 * @param status The status to filter by (e.g., PENDING, DELIVERED).
	 * @return A list of orders with the given status, ordered by date descending.
	 */
	Page<Order> findOrdersByStatus(String status, Pageable pageable); // Updated

	/**
	 * Searches orders based on a keyword (ID or customer name) and optionally
	 * filters by status.
	 *
	 * @param keyword The search term. Can be null or empty.
	 * @param status  The status to filter by. Can be null or empty.
	 * @return A list of matching orders, ordered by date descending.
	 */
	Page<Order> searchOrders(String keyword, String status, String startDate, String endDate, Pageable pageable); // Updated

	// --- NEW: For Dashboard ---

	/**
	 * Gets total sales for today (from 12:00 AM to now) from 'DELIVERED' orders.
	 * * @return Total sales as BigDecimal.
	 */
	BigDecimal getSalesToday();

	/**
	 * Gets total sales for this week (from Monday 12:00 AM to now) from 'DELIVERED'
	 * orders. * @return Total sales as BigDecimal.
	 */
	BigDecimal getSalesThisWeek();

	/**
	 * Gets total sales for this month (from 1st 12:00 AM to now) from 'DELIVERED'
	 * orders. * @return Total sales as BigDecimal.
	 */
	BigDecimal getSalesThisMonth();

	/**
	 * Gets a map of all order statuses and their counts. e.g., {"PENDING": 5,
	 * "DELIVERED": 10} * @return A Map<String, Long> of status counts.
	 */
	Map<String, Long> getOrderStatusCounts();

	/**
	 * Gets a list of top-selling products and the total quantity sold. * @param
	 * limit The number of top products to return (e.g., 5). * @return A List of
	 * Maps, where each map contains "product" (Product object) and "quantity"
	 * (Long).
	 */
	List<Map<String, Object>> getTopSellingProducts(int limit);

	/**
	 * Gets sales data grouped by day for a date range, for use in charts. * @param
	 * start Start timestamp. * @param end End timestamp. * @return A Map<String,
	 * BigDecimal> where key is date (YYYY-MM-DD) and value is total sales.
	 */
	Map<String, BigDecimal> getSalesDataForChart(LocalDateTime start, LocalDateTime end);

	// --- NEW: For Transaction History Page ---
	/**
	 * Gets the sum of all 'DELIVERED' orders ever. * @return Total revenue as
	 * BigDecimal.
	 */
	BigDecimal getTotalRevenueAllTime();

	/**
	 * Gets the count of all 'DELIVERED' orders ever. * @return Total number of
	 * transactions as long.
	 */
	long getTotalTransactionsAllTime();
}