package com.toastedsiopao.repository;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	// Find orders by User, ordered by date descending (newest first)
	List<Order> findByUserOrderByOrderDateDesc(User user);

	// Find all orders ordered by date descending (newest first) - Useful for admin
	List<Order> findAllByOrderByOrderDateDesc();

	// --- NEW: Find orders by status ---
	List<Order> findByStatusOrderByOrderDateDesc(String status);

	// --- NEW: Search orders by keyword (ID or customer name) ---
	@Query("SELECT o FROM Order o WHERE " + "CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') OR " // Search by ID
																										// (converted to
																										// string)
			+ "LOWER(o.shippingFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(o.shippingLastName) LIKE LOWER(CONCAT('%', :keyword, '%')) " + "ORDER BY o.orderDate DESC")
	List<Order> searchOrdersByKeyword(@Param("keyword") String keyword);

	// --- NEW: Search orders by keyword AND status ---
	@Query("SELECT o FROM Order o WHERE o.status = :status AND ("
			+ "CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') OR "
			+ "LOWER(o.shippingFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(o.shippingLastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " + "ORDER BY o.orderDate DESC")
	List<Order> searchOrdersByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status);

}