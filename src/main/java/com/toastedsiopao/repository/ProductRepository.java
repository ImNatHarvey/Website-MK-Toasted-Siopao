package com.toastedsiopao.repository;

import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // NEW IMPORT
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	// Find products by category
	Page<Product> findByCategory(Category category, Pageable pageable); // Updated

	// Find products by name containing (for search) - ignoring case
	Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable); // Updated

	// Find products by category name (useful for filtering)
	Page<Product> findByCategoryNameIgnoreCase(String categoryName, Pageable pageable); // Updated

	// Combine search and category filtering
	Page<Product> findByNameContainingIgnoreCaseAndCategoryNameIgnoreCase(String keyword, String categoryName,
			Pageable pageable); // Updated

	// --- NEW: Combined search by keyword and Category object (more efficient) ---
	Page<Product> findByNameContainingIgnoreCaseAndCategory(String keyword, Category category, Pageable pageable);

	// --- NEW: Methods for stat cards ---
	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= p.lowStockThreshold AND p.currentStock > p.criticalStockThreshold")
	long countLowStockProducts();

	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= 0")
	long countOutOfStockProducts();
	// --- END NEW ---

}