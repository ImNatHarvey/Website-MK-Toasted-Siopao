package com.toastedsiopao.repository;

import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; // --- IMPORT ADDED ---
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; 

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	// --- REMOVED: Problematic FIND_PRODUCT_WITH_RELATIONS string ---

	String COUNT_PRODUCT = "SELECT COUNT(p) FROM Product p ";
	
	// --- ADDED: Clauses for public-facing searches ---
	String ACTIVE_PRODUCT_CLAUSE = "WHERE p.productStatus = 'ACTIVE' ";
	String ACTIVE_PRODUCT_AND_CLAUSE = "AND p.productStatus = 'ACTIVE' ";
	// --- END ADDED ---
	
	// --- ADDED: Re-usable custom sorting ---
	String CUSTOM_SORT = "ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC";
	// --- END ADDED ---

	// --- ADDED ---
	Optional<Product> findByNameIgnoreCase(String name);

	// --- MODIFIED: Added @EntityGraph, removed JOIN FETCH from query value ---
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	@Query(value = "SELECT p FROM Product p " + CUSTOM_SORT, 
		   countQuery = COUNT_PRODUCT)
	Page<Product> findAll(Pageable pageable);

	// --- MODIFIED: Added @EntityGraph, removed JOIN FETCH from query value ---
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	@Query(value = "SELECT p FROM Product p "
			+ ACTIVE_PRODUCT_CLAUSE
			+ CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + ACTIVE_PRODUCT_CLAUSE)
	Page<Product> findAllActive(Pageable pageable);
	// --- END MODIFIED ---

	// --- MODIFIED: Added @EntityGraph, removed JOIN FETCH from query value ---
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	@Query(value = "SELECT p FROM Product p "
			+ "WHERE p.category = :category " + CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + "WHERE p.category = :category")
	Page<Product> findByCategory(@Param("category") Category category, Pageable pageable);
	
	// --- MODIFIED: Added @EntityGraph, removed JOIN FETCH from query value ---
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	@Query(value = "SELECT p FROM Product p "
			+ "WHERE p.category = :category "
			+ ACTIVE_PRODUCT_AND_CLAUSE
			+ CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + "WHERE p.category = :category " + ACTIVE_PRODUCT_AND_CLAUSE)
	Page<Product> findActiveByCategory(@Param("category") Category category, Pageable pageable);
	// --- END ADDED ---

	// --- MODIFIED: Added @EntityGraph, removed JOIN FETCH from query value ---
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	@Query(value = "SELECT p FROM Product p "
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Product> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);
	
	// --- MODIFIED: Added @EntityGraph, removed JOIN FETCH from query value ---
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	@Query(value = "SELECT p FROM Product p "
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ ACTIVE_PRODUCT_AND_CLAUSE
			+ CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + ACTIVE_PRODUCT_AND_CLAUSE)
	Page<Product> findActiveByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);
	// --- END ADDED ---

	// --- MODIFIED: Added @EntityGraph, removed JOIN FETCH from query value ---
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	@Query(value = "SELECT p FROM Product p "
			+ "WHERE LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.name ASC", 
			countQuery = COUNT_PRODUCT + "WHERE LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Product> findByCategoryNameIgnoreCase(@Param("keyword") String categoryName, Pageable pageable);

	// --- MODIFIED: Added @EntityGraph, removed JOIN FETCH from query value ---
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	@Query(value = "SELECT p FROM Product p "
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "AND p.category = :category " + CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + "AND p.category = :category")
	Page<Product> findByNameContainingIgnoreCaseAndCategory(@Param("keyword") String keyword,
			@Param("category") Category category, Pageable pageable);
			
	// --- MODIFIED: Added @EntityGraph, removed JOIN FETCH from query value ---
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	@Query(value = "SELECT p FROM Product p "
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "AND p.category = :category "
			+ ACTIVE_PRODUCT_AND_CLAUSE
			+ CUSTOM_SORT, 
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