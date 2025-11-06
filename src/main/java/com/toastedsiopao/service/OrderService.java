package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable

import java.util.List;
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
	Page<Order> searchOrders(String keyword, String status, Pageable pageable); // Updated

}