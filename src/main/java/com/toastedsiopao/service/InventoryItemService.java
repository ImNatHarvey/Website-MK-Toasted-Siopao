package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryItemDto;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.UnitOfMeasure; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal; 
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InventoryItemService {

	List<InventoryItem> findAll(); 
	
	List<InventoryItem> findAllActive();

	Page<InventoryItem> findAll(Pageable pageable); 

	Optional<InventoryItem> findById(Long id);

	InventoryItem save(InventoryItemDto itemDto); 

	void deactivateItem(Long id);
	
	void activateItem(Long id);
	
	void deleteItem(Long id);

	Optional<InventoryItem> findByName(String name);

	Page<InventoryItem> searchItems(String keyword, Long categoryId, Pageable pageable); 

	List<InventoryItem> findLowStockItems();

	List<InventoryItem> findCriticalStockItems();

	List<InventoryItem> findOutOfStockItems();

	InventoryItem adjustStock(Long itemId, BigDecimal quantityChange, String reason, LocalDate receivedDate, Integer expirationDays);

	default InventoryItem adjustStock(Long itemId, BigDecimal quantityChange, String reason) {
		return adjustStock(itemId, quantityChange, reason, null, null);
	}

	BigDecimal getTotalStockQuantity();

	BigDecimal getTotalStockValue();

	long countLowStockItems();

	long countCriticalStockItems();

	long countOutOfStockItems();

	long countByUnit(UnitOfMeasure unit);
	
	// --- ADDED ---
	Map<String, Object> getInventoryMetrics(String keyword, Long categoryId);
}