package com.toastedsiopao.repository;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.OrderItem;
import com.toastedsiopao.model.Product; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; 
import org.springframework.data.repository.query.Param; 
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	List<OrderItem> findByOrder(Order order);

	List<OrderItem> findByProduct(Product product);
	
	// --- THIS IS THE FIX ---
	@Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.product = :product")
	long countByProduct(@Param("product") Product product);
	// --- END FIX ---

}