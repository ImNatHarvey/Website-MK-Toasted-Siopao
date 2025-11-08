package com.toastedsiopao.repository;

import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.UnitOfMeasure; // NEW IMPORT
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

	String FIND_ITEM_WITH_RELATIONS = "SELECT i FROM InventoryItem i " + "JOIN FETCH i.category c "
			+ "JOIN FETCH i.unit u ";

	Optional<InventoryItem> findByNameIgnoreCase(String name);

	@Query(value = FIND_ITEM_WITH_RELATIONS + "ORDER BY i.name ASC")
	Page<InventoryItem> findAll(Pageable pageable);
	
	@Query(value = FIND_ITEM_WITH_RELATIONS + "WHERE i.category = :category ORDER BY i.name ASC")
	Page<InventoryItem> findByCategoryOrderByNameAsc(@Param("category") InventoryCategory category, Pageable pageable);

	@Query(value = FIND_ITEM_WITH_RELATIONS
			+ "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY i.name ASC")
	Page<InventoryItem> findByNameContainingIgnoreCaseOrderByNameAsc(@Param("keyword") String keyword,
			Pageable pageable);

	@Query(value = FIND_ITEM_WITH_RELATIONS
			+ "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND i.category = :category ORDER BY i.name ASC")
	Page<InventoryItem> findByNameContainingIgnoreCaseAndCategoryOrderByNameAsc(@Param("keyword") String keyword,
			@Param("category") InventoryCategory category, Pageable pageable);

	@Query(FIND_ITEM_WITH_RELATIONS + "ORDER BY i.name ASC")
	List<InventoryItem> findAllByOrderByNameAsc();

	@Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.lowStockThreshold AND i.currentStock > i.criticalStockThreshold ORDER BY i.name ASC")
	List<InventoryItem> findLowStockItems();

	@Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.criticalStockThreshold AND i.currentStock > 0 ORDER BY i.name ASC")
	List<InventoryItem> findCriticalStockItems();

	@Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= 0 ORDER BY i.name ASC")
	List<InventoryItem> findOutOfStockItems();

	@Query("SELECT COALESCE(SUM(i.currentStock), 0) FROM InventoryItem i")
	BigDecimal sumTotalStockQuantity();

	@Query("SELECT COALESCE(SUM(i.currentStock * i.costPerUnit), 0) FROM InventoryItem i")
	BigDecimal sumTotalStockValue();

	@Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.currentStock <= i.lowStockThreshold AND i.currentStock > i.criticalStockThreshold")
	long countLowStockItems();

	@Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.currentStock <= i.criticalStockThreshold AND i.currentStock > 0")
	long countCriticalStockItems();

	@Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.currentStock <= 0")
	long countOutOfStockItems();

	long countByUnit(UnitOfMeasure unit);
}