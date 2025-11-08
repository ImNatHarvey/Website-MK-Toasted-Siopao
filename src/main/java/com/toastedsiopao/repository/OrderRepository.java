package com.toastedsiopao.repository;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page; 
import org.springframework.data.domain.Pageable; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; 
import org.springframework.data.repository.query.Param; 
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findByUserOrderByOrderDateDesc(User user, Pageable pageable); 
	
	String ADMIN_ORDER_JOINS = "LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.product p ";

	@Query(value = "SELECT DISTINCT o FROM Order o " + ADMIN_ORDER_JOINS + "WHERE "
			+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) "
			+ "ORDER BY o.orderDate DESC", countQuery = "SELECT COUNT(o) FROM Order o WHERE "
					+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
					+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) ")
	Page<Order> findAllByDate(@Param("startDateTime") LocalDateTime startDateTime,
			@Param("endDateTime") LocalDateTime endDateTime, Pageable pageable); 

	@Query(value = "SELECT DISTINCT o FROM Order o " + ADMIN_ORDER_JOINS + "WHERE o.status = :status AND "
			+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) "
			+ "ORDER BY o.orderDate DESC", countQuery = "SELECT COUNT(o) FROM Order o WHERE o.status = :status AND "
					+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
					+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) ")
	Page<Order> searchByStatusAndDate(@Param("status") String status,
			@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
			Pageable pageable); 
	
	@Query(value = "SELECT DISTINCT o FROM Order o " + ADMIN_ORDER_JOINS + "WHERE "
			+ "(CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') OR "
			+ "LOWER(o.shippingFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(o.shippingLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
			+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) "
			+ "ORDER BY o.orderDate DESC", countQuery = "SELECT COUNT(o) FROM Order o WHERE "
					+ "(CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') OR "
					+ "LOWER(o.shippingFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
					+ "LOWER(o.shippingLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
					+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
					+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) ")
	Page<Order> searchOrdersByKeyword(@Param("keyword") String keyword,
			@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
			Pageable pageable); 

	@Query(value = "SELECT DISTINCT o FROM Order o " + ADMIN_ORDER_JOINS + "WHERE o.status = :status AND "
			+ "(CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') OR "
			+ "LOWER(o.shippingFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(o.shippingLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
			+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) "
			+ "ORDER BY o.orderDate DESC", countQuery = "SELECT COUNT(o) FROM Order o WHERE o.status = :status AND "
					+ "(CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') OR "
					+ "LOWER(o.shippingFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
					+ "LOWER(o.shippingLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
					+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
					+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) ")
	Page<Order> searchOrdersByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status,
			@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
			Pageable pageable); 

	@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :start AND :end AND o.status = 'DELIVERED'")
	BigDecimal findTotalSalesBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	@Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
	List<Object[]> countOrdersByStatus();

	@Query("SELECT oi.product, SUM(oi.quantity) AS totalQuantity FROM OrderItem oi JOIN oi.order o WHERE o.status = 'DELIVERED' GROUP BY oi.product ORDER BY totalQuantity DESC")
	Page<Object[]> findTopSellingProducts(Pageable pageable);

	@Query("SELECT FUNCTION('DATE', o.orderDate) AS orderDay, SUM(o.totalAmount) AS dailySales FROM Order o WHERE o.orderDate BETWEEN :start AND :end AND o.status = 'DELIVERED' GROUP BY orderDay ORDER BY orderDay ASC")
	List<Object[]> findSalesDataBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
	BigDecimal findTotalRevenueAllTime();

	@Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED'")
	long countTotalTransactionsAllTime();
}