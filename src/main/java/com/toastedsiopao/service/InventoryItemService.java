package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryItemDto;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.UnitOfMeasure; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal; 
import java.util.List;
import java.util.Optional;

public interface InventoryItemService {

	List<InventoryItem> findAll(); 

	Page<InventoryItem> findAll(Pageable pageable); 

	Optional<InventoryItem> findById(Long id);

	InventoryItem save(InventoryItemDto itemDto); 

	// --- MODIFIED ---
	void deactivateItem(Long id);
	
	void activateItem(Long id);
	
	void deleteItem(Long id); // --- ADDED ---
	// --- END MODIFIED ---

	Optional<InventoryItem> findByName(String name);

	Page<InventoryItem> searchItems(String keyword, Long categoryId, Pageable pageable); 

	List<InventoryItem> findLowStockItems();

	List<InventoryItem> findCriticalStockItems();

	List<InventoryItem> findOutOfStockItems();

	InventoryItem adjustStock(Long itemId, BigDecimal quantityChange, String reason);

	BigDecimal getTotalStockQuantity();

	BigDecimal getTotalStockValue();

	long countLowStockItems();

	long countCriticalStockItems();

	long countOutOfStockItems();

	long countByUnit(UnitOfMeasure unit);
}