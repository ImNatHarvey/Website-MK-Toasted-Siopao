package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
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
	public Page<Order> findOrdersByUser(User user, Pageable pageable) { // Updated
		return orderRepository.findByUserOrderByOrderDateDesc(user, pageable); // Updated
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Order> findAllOrders(Pageable pageable) { // Updated
		return orderRepository.findAllByOrderByOrderDateDesc(pageable); // Updated
	}

	// --- NEW: Implementation for finding by status ---
	@Override
	@Transactional(readOnly = true)
	public Page<Order> findOrdersByStatus(String status, Pageable pageable) { // Updated
		// Ensure status is not null/empty before querying
		if (!StringUtils.hasText(status)) {
			return findAllOrders(pageable); // Updated
		}
		return orderRepository.findByStatusOrderByOrderDateDesc(status.toUpperCase(), pageable); // Updated
	}

	// --- NEW: Implementation for searching/filtering orders ---
	@Override
	@Transactional(readOnly = true)
	public Page<Order> searchOrders(String keyword, String status, Pageable pageable) { // Updated
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasStatus = StringUtils.hasText(status);
		String upperStatus = hasStatus ? status.toUpperCase() : null;

		if (hasKeyword && hasStatus) {
			return orderRepository.searchOrdersByKeywordAndStatus(keyword.trim(), upperStatus, pageable); // Updated
		} else if (hasKeyword) {
			return orderRepository.searchOrdersByKeyword(keyword.trim(), pageable); // Updated
		} else if (hasStatus) {
			return orderRepository.findByStatusOrderByOrderDateDesc(upperStatus, pageable); // Updated
		} else {
			return findAllOrders(pageable); // No keyword, no status -> return all // Updated
		}
	}
}