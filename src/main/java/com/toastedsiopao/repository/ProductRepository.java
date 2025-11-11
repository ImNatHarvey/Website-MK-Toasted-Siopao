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

	// --- ADDED ---
	Optional<Product> findByNameIgnoreCase(String name);

	@Query(value = FIND_PRODUCT_WITH_RELATIONS + "ORDER BY p.name ASC", countQuery = COUNT_PRODUCT)
	Page<Product> findAll(Pageable pageable);

	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE p.category = :category ORDER BY p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE p.category = :category")
	Page<Product> findByCategory(@Param("category") Category category, Pageable pageable);

	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Product> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

	@Query(value = FIND_PRODUCT_WITH_RELATIONS
			+ "WHERE LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Product> findByCategoryNameIgnoreCase(@Param("keyword") String categoryName, Pageable pageable);

	@Query(value = FIND_PRODUCT_WITH_RELATIONS + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "AND p.category = :category ORDER BY p.name ASC", countQuery = COUNT_PRODUCT
					+ "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " + "AND p.category = :category")
	Page<Product> findByNameContainingIgnoreCaseAndCategory(@Param("keyword") String keyword,
			@Param("category") Category category, Pageable pageable);

	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= p.lowStockThreshold AND p.currentStock > p.criticalStockThreshold")
	long countLowStockProducts();

	@Query("SELECT count(p) FROM Product p WHERE p.currentStock <= 0")
	long countOutOfStockProducts();

}