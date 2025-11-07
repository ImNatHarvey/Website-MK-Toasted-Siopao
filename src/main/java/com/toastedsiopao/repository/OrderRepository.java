package com.toastedsiopao.repository;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	// Find orders by User, ordered by date descending (newest first)
	Page<Order> findByUserOrderByOrderDateDesc(User user, Pageable pageable); // Updated

	// Find all orders ordered by date descending (newest first) - Useful for admin
	@Query("SELECT o FROM Order o WHERE " + "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) " + "ORDER BY o.orderDate DESC")
	Page<Order> findAllByDate(@Param("startDateTime") LocalDateTime startDateTime,
			@Param("endDateTime") LocalDateTime endDateTime, Pageable pageable); // Updated

	// --- NEW: Find orders by status ---
	@Query("SELECT o FROM Order o WHERE o.status = :status AND "
			+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) " + "ORDER BY o.orderDate DESC")
	Page<Order> searchByStatusAndDate(@Param("status") String status,
			@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
			Pageable pageable); // Updated

	// --- NEW: Search orders by keyword (ID or customer name) ---
	@Query("SELECT o FROM Order o WHERE " + "(CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') OR "
			+ "LOWER(o.shippingFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(o.shippingLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
			+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) " + "ORDER BY o.orderDate DESC")
	Page<Order> searchOrdersByKeyword(@Param("keyword") String keyword,
			@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
			Pageable pageable); // Updated

	// --- NEW: Search orders by keyword AND status ---
	@Query("SELECT o FROM Order o WHERE o.status = :status AND "
			+ "(CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') OR "
			+ "LOWER(o.shippingFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(o.shippingLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
			+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) " + "ORDER BY o.orderDate DESC")
	Page<Order> searchOrdersByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status,
			@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
			Pageable pageable); // Updated

	// --- NEW: For Dashboard Stats ---

	/**
	 * Calculates the sum of totalAmount for all orders between two timestamps with
	 * status 'DELIVERED'. COALESCE is used to return 0 instead of null if no orders
	 * are found.
	 */
	@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :start AND :end AND o.status = 'DELIVERED'")
	BigDecimal findTotalSalesBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	/**
	 * Counts orders grouped by their status. Returns a List of Object arrays, where
	 * each array is [String status, Long count].
	 */
	@Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
	List<Object[]> countOrdersByStatus();

	/**
	 * Finds the top-selling products based on the sum of quantity from 'DELIVERED'
	 * orders. Returns a Page of Object arrays, where each array is [Product
	 * product, Long totalQuantity].
	 */
	@Query("SELECT oi.product, SUM(oi.quantity) AS totalQuantity FROM OrderItem oi JOIN oi.order o WHERE o.status = 'DELIVERED' GROUP BY oi.product ORDER BY totalQuantity DESC")
	Page<Object[]> findTopSellingProducts(Pageable pageable);

	/**
	 * Calculates the sum of sales per day for a given date range, for 'DELIVERED'
	 * orders. Returns a List of Object arrays, where each array is [java.sql.Date
	 * orderDay, BigDecimal dailySales].
	 */
	@Query("SELECT FUNCTION('DATE', o.orderDate) AS orderDay, SUM(o.totalAmount) AS dailySales FROM Order o WHERE o.orderDate BETWEEN :start AND :end AND o.status = 'DELIVERED' GROUP BY orderDay ORDER BY orderDay ASC")
	List<Object[]> findSalesDataBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	// --- NEW: For Transaction History Page ---
	/**
	 * Calculates the sum of totalAmount for all 'DELIVERED' orders.
	 */
	@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
	BigDecimal findTotalRevenueAllTime();

	/**
	 * Counts all 'DELIVERED' orders.
	 */
	@Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED'")
	long countTotalTransactionsAllTime();
}