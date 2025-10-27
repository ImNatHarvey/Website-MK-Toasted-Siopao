package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryItemDto;
import com.toastedsiopao.model.InventoryItem;
import java.util.List;
import java.util.Optional;

public interface InventoryItemService {

	List<InventoryItem> findAll();

	Optional<InventoryItem> findById(Long id);

	InventoryItem save(InventoryItemDto itemDto); // Save/Update using DTO

	void deleteById(Long id);

	Optional<InventoryItem> findByName(String name);

	List<InventoryItem> searchItems(String keyword, Long categoryId);

	// Methods for stock reports
	List<InventoryItem> findLowStockItems();

	List<InventoryItem> findCriticalStockItems();

	List<InventoryItem> findOutOfStockItems();

	// Method to adjust stock (e.g., when ingredients are used or received)
	// We'll implement the logic later
	// void adjustStock(Long itemId, BigDecimal quantityChange, String reason);
}