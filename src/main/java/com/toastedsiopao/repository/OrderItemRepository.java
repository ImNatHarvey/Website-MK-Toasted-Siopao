package com.toastedsiopao.repository;

import com.toastedsiopao.model.Order; // Import Order
import com.toastedsiopao.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Import List

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	// Find all items belonging to a specific order
	List<OrderItem> findByOrder(Order order);

}