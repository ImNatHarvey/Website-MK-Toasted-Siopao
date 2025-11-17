package com.toastedsiopao.repository;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	@Query(value = "SELECT o.id FROM Order o "
			+ "WHERE o.user = :user ORDER BY o.orderDate DESC",
			countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user = :user")
	Page<Long> findIdsByUserOrderByOrderDateDesc(@Param("user") User user, Pageable pageable);

	@Query(value = "SELECT o.id FROM Order o "
			+ "WHERE o.user = :user AND o.status = :status ORDER BY o.orderDate DESC",
			countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user = :user AND o.status = :status")
	Page<Long> findIdsByUserAndStatusOrderByOrderDateDesc(@Param("user") User user, @Param("status") String status, Pageable pageable);

	@Query(value = "SELECT o.id FROM Order o WHERE "
			+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) "
			+ "ORDER BY o.orderDate DESC", countQuery = "SELECT COUNT(o) FROM Order o WHERE "
					+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
					+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) ")
	Page<Long> findIdsByDate(@Param("startDateTime") LocalDateTime startDateTime,
			@Param("endDateTime") LocalDateTime endDateTime, Pageable pageable);

	@Query(value = "SELECT o.id FROM Order o WHERE o.status = :status AND "
			+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
			+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) "
			+ "ORDER BY o.orderDate DESC", countQuery = "SELECT COUNT(o) FROM Order o WHERE o.status = :status AND "
					+ "(:startDateTime IS NULL OR o.orderDate >= :startDateTime) AND "
					+ "(:endDateTime IS NULL OR o.orderDate <= :endDateTime) ")
	Page<Long> findIdsByStatusAndDate(@Param("status") String status,
			@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
			Pageable pageable);

	@Query(value = "SELECT o.id FROM Order o WHERE "
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
	Page<Long> findIdsByKeyword(@Param("keyword") String keyword,
			@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
			Pageable pageable);

	@Query(value = "SELECT o.id FROM Order o WHERE o.status = :status AND "
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
	Page<Long> findIdsByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status,
			@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
			Pageable pageable);
	
	@Query("SELECT o FROM Order o "
			+ "LEFT JOIN FETCH o.items oi "
			+ "LEFT JOIN FETCH oi.product p "
			+ "LEFT JOIN FETCH o.issueReports "
			+ "WHERE o.id IN :ids ORDER BY o.orderDate DESC")
	@EntityGraph(attributePaths = {"items", "items.product", "issueReports"})
	List<Order> findWithDetailsByIds(@Param("ids") List<Long> ids);

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

	@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN ('PENDING', 'PENDING_VERIFICATION', 'PROCESSING', 'OUT_FOR_DELIVERY')")
	BigDecimal getTotalPotentialRevenue();
	
	@Query("SELECT DISTINCT o FROM Order o "
			+ "JOIN FETCH o.items oi "
			+ "JOIN oi.product p "
			+ "LEFT JOIN FETCH p.ingredients ri "
			+ "LEFT JOIN FETCH ri.inventoryItem ii "
			+ "WHERE o.status = 'DELIVERED' "
			+ "AND (:start IS NULL OR o.orderDate >= :start) " 
			+ "AND (:end IS NULL OR o.orderDate <= :end) "     
			+ "AND (:keyword IS NULL OR "                     
			+ "    CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') OR "
			+ "    LOWER(o.shippingFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "    LOWER(o.shippingLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))"
			+ ") "
			+ "ORDER BY o.orderDate DESC")
	List<Order> findDeliveredOrdersWithCogsDetails(@Param("keyword") String keyword, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
	
	@Query("SELECT o FROM Order o LEFT JOIN FETCH o.user u LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.product p WHERE o.id = :orderId")
	Optional<Order> findOrderForInvoiceById(@Param("orderId") Long orderId);
}