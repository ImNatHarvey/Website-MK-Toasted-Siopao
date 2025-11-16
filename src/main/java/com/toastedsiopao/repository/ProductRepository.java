package com.toastedsiopao.repository;

import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock; // --- ADDED ---
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType; // --- ADDED ---
import java.util.List;
import java.util.Optional; 

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	String COUNT_PRODUCT = "SELECT COUNT(p) FROM Product p ";
	
	String ACTIVE_PRODUCT_CLAUSE = "WHERE p.productStatus = 'ACTIVE' ";
	String ACTIVE_PRODUCT_AND_CLAUSE = "AND p.productStatus = 'ACTIVE' ";
	
	String CUSTOM_SORT = "ORDER BY (CASE WHEN p.currentStock > 0 THEN 0 ELSE 1 END), p.name ASC";

	Optional<Product> findByNameIgnoreCase(String name);

	// --- START: MODIFIED 2-STEP PAGINATION QUERIES ---

	// 1. Find Paginated IDs

	@Query(value = "SELECT p.id FROM Product p " + CUSTOM_SORT, 
		   countQuery = COUNT_PRODUCT)
	Page<Long> findIdsAll(Pageable pageable);

	@Query(value = "SELECT p.id FROM Product p "
			+ ACTIVE_PRODUCT_CLAUSE
			+ CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + ACTIVE_PRODUCT_CLAUSE)
	Page<Long> findIdsAllActive(Pageable pageable);

	@Query(value = "SELECT p.id FROM Product p "
			+ "WHERE p.category = :category " + CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + "WHERE p.category = :category")
	Page<Long> findIdsByCategory(@Param("category") Category category, Pageable pageable);
	
	@Query(value = "SELECT p.id FROM Product p "
			+ "WHERE p.category = :category "
			+ ACTIVE_PRODUCT_AND_CLAUSE
			+ CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + "WHERE p.category = :category " + ACTIVE_PRODUCT_AND_CLAUSE)
	Page<Long> findIdsActiveByCategory(@Param("category") Category category, Pageable pageable);

	@Query(value = "SELECT p.id FROM Product p "
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Long> findIdsByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);
	
	@Query(value = "SELECT p.id FROM Product p "
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ ACTIVE_PRODUCT_AND_CLAUSE
			+ CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + ACTIVE_PRODUCT_AND_CLAUSE)
	Page<Long> findIdsActiveByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

	@Query(value = "SELECT p.id FROM Product p "
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "AND p.category = :category " + CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + "AND p.category = :category")
	Page<Long> findIdsByNameContainingIgnoreCaseAndCategory(@Param("keyword") String keyword,
			@Param("category") Category category, Pageable pageable);
			
	@Query(value = "SELECT p.id FROM Product p "
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "AND p.category = :category "
			+ ACTIVE_PRODUCT_AND_CLAUSE
			+ CUSTOM_SORT, 
			countQuery = COUNT_PRODUCT + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " 
			+ "AND p.category = :category " + ACTIVE_PRODUCT_AND_CLAUSE)
	Page<Long> findIdsActiveByNameContainingIgnoreCaseAndCategory(@Param("keyword") String keyword,
			@Param("category") Category category, Pageable pageable);

	// 2. Find Details for those IDs
	
	@Query("SELECT p FROM Product p WHERE p.id IN :ids " + CUSTOM_SORT)
	@EntityGraph(attributePaths = {"category", "ingredients.inventoryItem.unit"})
	List<Product> findWithDetailsByIds(@Param("ids") List<Long> ids);

	// --- THIS IS THE FIX ---
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Product p WHERE p.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") Long id);
	// --- END FIX ---

	// --- END: MODIFIED 2-STEP PAGINATION QUERIES ---

	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= p.lowStockThreshold AND p.currentStock > p.criticalStockThreshold")
	long countLowStockProducts();

	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= 0")
	long countOutOfStockProducts();

	// --- REMOVED: Old paginated queries that caused the warning ---

	// === NEW QUERIES FOR PRODUCT REPORT ===
	@Query("SELECT p FROM Product p JOIN FETCH p.category c LEFT JOIN FETCH p.ingredients i LEFT JOIN FETCH i.inventoryItem ii LEFT JOIN FETCH ii.unit u ORDER BY p.name ASC")
	List<Product> findAllFullProducts();

	@Query("SELECT p FROM Product p JOIN FETCH p.category c LEFT JOIN FETCH p.ingredients i LEFT JOIN FETCH i.inventoryItem ii LEFT JOIN FETCH ii.unit u WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.name ASC")
	List<Product> findFullProductsByName(@Param("keyword") String keyword);

	@Query("SELECT p FROM Product p JOIN FETCH p.category c LEFT JOIN FETCH p.ingredients i LEFT JOIN FETCH i.inventoryItem ii LEFT JOIN FETCH ii.unit u WHERE p.category = :category ORDER BY p.name ASC")
	List<Product> findFullProductsByCategory(@Param("category") Category category);

	@Query("SELECT p FROM Product p JOIN FETCH p.category c LEFT JOIN FETCH p.ingredients i LEFT JOIN FETCH i.inventoryItem ii LEFT JOIN FETCH ii.unit u WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.category = :category ORDER BY p.name ASC")
	List<Product> findFullProductsByNameAndCategory(@Param("keyword") String keyword, @Param("category") Category category);
	// =======================================
}