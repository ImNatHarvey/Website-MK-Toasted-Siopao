package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;

import java.util.List;
import java.util.Optional;

public interface OrderService {

	Optional<Order> findOrderById(Long id);

	List<Order> findOrdersByUser(User user);

	List<Order> findAllOrders();

	// --- NEW METHODS ---
	/**
	 * Finds all orders matching a specific status.
	 *
	 * @param status The status to filter by (e.g., PENDING, DELIVERED).
	 * @return A list of orders with the given status, ordered by date descending.
	 */
	List<Order> findOrdersByStatus(String status);

	/**
	 * Searches orders based on a keyword (ID or customer name) and optionally
	 * filters by status.
	 *
	 * @param keyword The search term. Can be null or empty.
	 * @param status  The status to filter by. Can be null or empty.
	 * @return A list of matching orders, ordered by date descending.
	 */
	List<Order> searchOrders(String keyword, String status);

}