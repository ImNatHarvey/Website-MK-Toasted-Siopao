/*
File: imnatharvey/website-mk-toasted-siopao/Website-MK-Toasted-Siopao-2de826f8fd7cd99b65487feb9dadc213b6ecccd9/src/main/java/com/toastedsiopao/repository/ProductRepository.java
*/
package com.toastedsiopao.repository;

import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // NEW IMPORT
import org.springframework.data.repository.query.Param; // NEW IMPORT
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	// --- BASE QUERY TO FETCH PRODUCTS WITH ALL RELATIONS ---
	// This query joins product (p) with its category (c)
	// and LEFT JOINs its ingredients list (i),
	// and for each ingredient, its inventory item (ii),
	// and for each item, its unit (u).
	// DISTINCT is crucial to prevent duplicate Products due to the 'ingredients'
	// join.
	String FIND_PRODUCT_WITH_RELATIONS = "SELECT DISTINCT p FROM Product p " + "JOIN FETCH p.category c "
			+ "LEFT JOIN FETCH p.ingredients i " + "LEFT JOIN FETCH i.inventoryItem ii " + "LEFT JOIN FETCH ii.unit u ";

	// --- COUNT QUERY FOR PAGINATION ---
	// This count query MUST NOT join the @OneToMany 'ingredients' list,
	// as it would return an incorrect count.
	String COUNT_PRODUCT = "SELECT COUNT(p) FROM Product p ";

	// --- OVERRIDE findAll to use JOIN FETCH ---
	@Query(value = FIND_PRODUCT_WITH_RELATIONS + "ORDER BY p.name ASC", countQuery = COUNT_PRODUCT)
	Page<Product> findAll(Pageable pageable);

	// Find products by category
	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE p.category = :category ORDER BY p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE p.category = :category")
	Page<Product> findByCategory(@Param("category") Category category, Pageable pageable); // Updated

	// Find products by name containing (for search) - ignoring case
	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Product> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable); // Updated

	// Find products by category name (useful for filtering)
	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Product> findByCategoryNameIgnoreCase(@Param("keyword") String categoryName, Pageable pageable); // Updated

	// Combine search and category filtering
	@Query(value = FIND_PRODUCT_WITH_RELATIONS + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "AND p.category = :category ORDER BY p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + "AND p.category = :category")
	Page<Product> findByNameContainingIgnoreCaseAndCategory(@Param("keyword") String keyword,
			@Param("category") Category category, Pageable pageable);

	// --- REMOVED unused method ---
	// Page<Product>
	// findByNameContainingIgnoreCaseAndCategoryNameIgnoreCase(String
	// keyword, String categoryName,
	// Pageable pageable);

	// --- NEW: Methods for stat cards ---
	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= p.lowStockThreshold AND p.currentStock > p.criticalStockThreshold")
	long countLowStockProducts();

	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= 0")
	long countOutOfStockProducts();
	// --- END NEW ---

}