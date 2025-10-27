package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Import StringUtils

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderRepository orderRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<Order> findOrderById(Long id) {
		return orderRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Order> findOrdersByUser(User user) {
		return orderRepository.findByUserOrderByOrderDateDesc(user);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Order> findAllOrders() {
		return orderRepository.findAllByOrderByOrderDateDesc();
	}

	// --- NEW: Implementation for finding by status ---
	@Override
	@Transactional(readOnly = true)
	public List<Order> findOrdersByStatus(String status) {
		// Ensure status is not null/empty before querying
		if (!StringUtils.hasText(status)) {
			return findAllOrders(); // Or maybe return an empty list? Decide based on UX.
		}
		return orderRepository.findByStatusOrderByOrderDateDesc(status.toUpperCase()); // Store/compare status in
																						// uppercase
	}

	// --- NEW: Implementation for searching/filtering orders ---
	@Override
	@Transactional(readOnly = true)
	public List<Order> searchOrders(String keyword, String status) {
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasStatus = StringUtils.hasText(status);
		String upperStatus = hasStatus ? status.toUpperCase() : null;

		if (hasKeyword && hasStatus) {
			return orderRepository.searchOrdersByKeywordAndStatus(keyword.trim(), upperStatus);
		} else if (hasKeyword) {
			return orderRepository.searchOrdersByKeyword(keyword.trim());
		} else if (hasStatus) {
			return orderRepository.findByStatusOrderByOrderDateDesc(upperStatus);
		} else {
			return findAllOrders(); // No keyword, no status -> return all
		}
	}
}