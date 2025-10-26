package com.toastedsiopao.repository;

import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	// Find products by category
	List<Product> findByCategory(Category category);

	// Find products by name containing (for search) - ignoring case
	List<Product> findByNameContainingIgnoreCase(String keyword);

	// Find products by category name (useful for filtering)
	List<Product> findByCategoryNameIgnoreCase(String categoryName);

	// Combine search and category filtering
	List<Product> findByNameContainingIgnoreCaseAndCategoryNameIgnoreCase(String keyword, String categoryName);

}
