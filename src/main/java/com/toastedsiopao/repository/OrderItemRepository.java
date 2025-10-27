package com.toastedsiopao.repository;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.OrderItem;
import com.toastedsiopao.model.Product; // NEW IMPORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	// Find all items belonging to a specific order
	List<OrderItem> findByOrder(Order order);

	// --- NEW METHOD ---
	// Find all order items that contain a specific product.
	// This is used to prevent product deletion if it's part of an order.
	List<OrderItem> findByProduct(Product product);

}