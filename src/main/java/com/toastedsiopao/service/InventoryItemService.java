package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryItemDto;
import com.toastedsiopao.model.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal; // Import BigDecimal
import java.util.List;
import java.util.Optional;

public interface InventoryItemService {

	List<InventoryItem> findAll(); // This remains for dropdowns/modals

	Page<InventoryItem> findAll(Pageable pageable); // NEW: For paginated view

	Optional<InventoryItem> findById(Long id);

	InventoryItem save(InventoryItemDto itemDto); // Save/Update using DTO

	void deleteById(Long id);

	Optional<InventoryItem> findByName(String name);

	Page<InventoryItem> searchItems(String keyword, Long categoryId, Pageable pageable); // UPDATED

	// Methods for stock reports
	List<InventoryItem> findLowStockItems();

	List<InventoryItem> findCriticalStockItems();

	List<InventoryItem> findOutOfStockItems();

	// --- UNCOMMENTED ---
	/**
	 * Adjusts the stock of an inventory item. * @param itemId The ID of the item
	 * 
	 * @param quantityChange The amount to add (positive) or remove (negative)
	 * @param reason         The reason for the adjustment
	 * @return The updated InventoryItem
	 */
	InventoryItem adjustStock(Long itemId, BigDecimal quantityChange, String reason);
}