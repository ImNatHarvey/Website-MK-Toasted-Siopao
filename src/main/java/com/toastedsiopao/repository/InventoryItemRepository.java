/*
File: imnatharvey/website-mk-toasted-siopao/Website-MK-Toasted-Siopao-2de826f8fd7cd99b65487feb9dadc213b6ecccd9/src/main/java/com/toastedsiopao/repository/InventoryItemRepository.java
*/
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

	// --- BASE QUERY TO FETCH ITEMS WITH RELATIONS ---
	// This query joins item (i) with its category (c) and unit (u).
	String FIND_ITEM_WITH_RELATIONS = "SELECT i FROM InventoryItem i " + "JOIN FETCH i.category c "
			+ "JOIN FETCH i.unit u ";

	// Find by name (for duplicate checks)
	Optional<InventoryItem> findByNameIgnoreCase(String name);

	// --- OVERRIDE findAll(Pageable) to use JOIN FETCH ---
	@Query(value = FIND_ITEM_WITH_RELATIONS + "ORDER BY i.name ASC")
	Page<InventoryItem> findAll(Pageable pageable);

	// Find items by category
	@Query(value = FIND_ITEM_WITH_RELATIONS + "WHERE i.category = :category ORDER BY i.name ASC")
	Page<InventoryItem> findByCategoryOrderByNameAsc(@Param("category") InventoryCategory category, Pageable pageable);

	// Search items by name containing keyword
	@Query(value = FIND_ITEM_WITH_RELATIONS
			+ "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY i.name ASC")
	Page<InventoryItem> findByNameContainingIgnoreCaseOrderByNameAsc(@Param("keyword") String keyword,
			Pageable pageable);

	// Search items by name containing keyword AND category
	@Query(value = FIND_ITEM_WITH_RELATIONS
			+ "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND i.category = :category ORDER BY i.name ASC")
	Page<InventoryItem> findByNameContainingIgnoreCaseAndCategoryOrderByNameAsc(@Param("keyword") String keyword,
			@Param("category") InventoryCategory category, Pageable pageable);

	// --- NEW: For modals, get all items sorted ---
	@Query(FIND_ITEM_WITH_RELATIONS + "ORDER BY i.name ASC")
	List<InventoryItem> findAllByOrderByNameAsc();

	// Find items with stock below or equal to a threshold (useful for reports)
	@Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.lowStockThreshold AND i.currentStock > i.criticalStockThreshold ORDER BY i.name ASC")
	List<InventoryItem> findLowStockItems();

	@Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.criticalStockThreshold AND i.currentStock > 0 ORDER BY i.name ASC")
	List<InventoryItem> findCriticalStockItems();

	@Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= 0 ORDER BY i.name ASC")
	List<InventoryItem> findOutOfStockItems();

	// --- NEW: For Dashboard Stats ---

	/**
	 * Calculates the sum of all 'currentStock' from all inventory items. * @return
	 * Total stock quantity as BigDecimal.
	 */
	@Query("SELECT COALESCE(SUM(i.currentStock), 0) FROM InventoryItem i")
	BigDecimal sumTotalStockQuantity();

	/**
	 * Calculates the total value of all inventory (stock * cost) * @return Total
	 * stock value as BigDecimal.
	 */
	@Query("SELECT COALESCE(SUM(i.currentStock * i.costPerUnit), 0) FROM InventoryItem i")
	BigDecimal sumTotalStockValue();

	/**
	 * Counts items that are low on stock.
	 */
	@Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.currentStock <= i.lowStockThreshold AND i.currentStock > i.criticalStockThreshold")
	long countLowStockItems();

	/**
	 * Counts items that are critically low on stock (but not out of stock).
	 */
	@Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.currentStock <= i.criticalStockThreshold AND i.currentStock > 0")
	long countCriticalStockItems();

	/**
	 * Counts items that are out of stock.
	 */
	@Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.currentStock <= 0")
	long countOutOfStockItems();

	// --- NEW: For deletion check ---
	/**
	 * Counts the number of inventory items associated with a specific unit.
	 * * @param unit The UnitOfMeasure to check for.
	 * 
	 * @return The count of items using this unit.
	 */
	long countByUnit(UnitOfMeasure unit);
}