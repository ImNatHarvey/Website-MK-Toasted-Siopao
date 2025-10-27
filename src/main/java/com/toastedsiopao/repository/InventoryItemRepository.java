package com.toastedsiopao.repository;

import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

	// Find by name (for duplicate checks)
	Optional<InventoryItem> findByNameIgnoreCase(String name);

	// Find items by category
	List<InventoryItem> findByCategoryOrderByNameAsc(InventoryCategory category);

	// Search items by name containing keyword
	List<InventoryItem> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword);

	// Search items by name containing keyword AND category
	List<InventoryItem> findByNameContainingIgnoreCaseAndCategoryOrderByNameAsc(String keyword,
			InventoryCategory category);

	// Find items with stock below or equal to a threshold (useful for reports)
	@Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.lowStockThreshold AND i.currentStock > i.criticalStockThreshold ORDER BY i.name ASC")
	List<InventoryItem> findLowStockItems();

	@Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.criticalStockThreshold AND i.currentStock > 0 ORDER BY i.name ASC")
	List<InventoryItem> findCriticalStockItems();

	@Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= 0 ORDER BY i.name ASC")
	List<InventoryItem> findOutOfStockItems();

}