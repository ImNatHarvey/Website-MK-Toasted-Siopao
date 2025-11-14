package com.toastedsiopao.repository;

import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // --- ADDED ---

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	String FIND_PRODUCT_WITH_RELATIONS = "SELECT DISTINCT p FROM Product p " + "JOIN FETCH p.category c "
			+ "LEFT JOIN FETCH p.ingredients i " + "LEFT JOIN FETCH i.inventoryItem ii " + "LEFT JOIN FETCH ii.unit u ";

	String COUNT_PRODUCT = "SELECT COUNT(p) FROM Product p ";
	
	// --- ADDED: Clauses for public-facing searches ---
	String ACTIVE_PRODUCT_CLAUSE = "WHERE p.productStatus = 'ACTIVE' ";
	String ACTIVE_PRODUCT_AND_CLAUSE = "AND p.productStatus = 'ACTIVE' ";
	// --- END ADDED ---

	// --- ADDED ---
	Optional<Product> findByNameIgnoreCase(String name);

	// --- MODIFIED: Added stock-based sorting ---
	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC", countQuery = COUNT_PRODUCT)
	Page<Product> findAll(Pageable pageable);

	// --- MODIFIED: Added stock-based sorting AND ACTIVE clause ---
	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ ACTIVE_PRODUCT_CLAUSE
			+ "ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC", 
			countQuery = COUNT_PRODUCT + ACTIVE_PRODUCT_CLAUSE)
	Page<Product> findAllActive(Pageable pageable);
	// --- END MODIFIED ---

	// --- MODIFIED: Added stock-based sorting ---
	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE p.category = :category ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE p.category = :category")
	Page<Product> findByCategory(@Param("category") Category category, Pageable pageable);
	
	// --- ADDED: New query for public-facing category search ---
	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE p.category = :category "
			+ ACTIVE_PRODUCT_AND_CLAUSE
			+ "ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC", 
			countQuery = COUNT_PRODUCT + "WHERE p.category = :category " + ACTIVE_PRODUCT_AND_CLAUSE)
	Page<Product> findActiveByCategory(@Param("category") Category category, Pageable pageable);
	// --- END ADDED ---

	// --- MODIFIED: Added stock-based sorting ---
	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Product> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);
	
	// --- ADDED: New query for public-facing keyword search ---
	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ ACTIVE_PRODUCT_AND_CLAUSE
			+ "ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC", 
			countQuery = COUNT_PRODUCT + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + ACTIVE_PRODUCT_AND_CLAUSE)
	Page<Product> findActiveByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);
	// --- END ADDED ---

	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Product> findByCategoryNameIgnoreCase(@Param("keyword") String categoryName, Pageable pageable);

	// --- MODIFIED: Added stock-based sorting ---
	@Query(value = FIND_PRODUCT_WITH_RELATIONS + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "AND p.category = :category ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + "AND p.category = :category")
	Page<Product> findByNameContainingIgnoreCaseAndCategory(@Param("keyword") String keyword,
			@Param("category") Category category, Pageable pageable);
			
	// --- ADDED: New query for public-facing combined search ---
	@Query(value = FIND_PRODUCT_WITH_RELATIONS + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "AND p.category = :category "
			+ ACTIVE_PRODUCT_AND_CLAUSE
			+ "ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC", 
			countQuery = COUNT_PRODUCT + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " 
			+ "AND p.category = :category " + ACTIVE_PRODUCT_AND_CLAUSE)
	Page<Product> findActiveByNameContainingIgnoreCaseAndCategory(@Param("keyword") String keyword,
			@Param("category") Category category, Pageable pageable);
	// --- END ADDED ---

	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= p.lowStockThreshold AND p.currentStock > p.criticalStockThreshold")
	long countLowStockProducts();

	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= 0")
	long countOutOfStockProducts();

}