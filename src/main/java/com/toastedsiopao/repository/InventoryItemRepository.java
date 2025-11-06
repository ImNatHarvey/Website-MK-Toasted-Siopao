package com.toastedsiopao.repository;

import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem;
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

	// Find by name (for duplicate checks)
	Optional<InventoryItem> findByNameIgnoreCase(String name);

	// Find items by category
	Page<InventoryItem> findByCategoryOrderByNameAsc(InventoryCategory category, Pageable pageable);

	// Search items by name containing keyword
	Page<InventoryItem> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword, Pageable pageable);

	// Search items by name containing keyword AND category
	Page<InventoryItem> findByNameContainingIgnoreCaseAndCategoryOrderByNameAsc(String keyword,
			InventoryCategory category, Pageable pageable);

	// --- NEW: For modals, get all items sorted ---
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
}