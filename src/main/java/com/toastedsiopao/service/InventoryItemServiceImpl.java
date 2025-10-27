package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryItemDto;
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.RecipeIngredient; // NEW
import com.toastedsiopao.model.UnitOfMeasure;
import com.toastedsiopao.repository.InventoryCategoryRepository;
import com.toastedsiopao.repository.InventoryItemRepository;
import com.toastedsiopao.repository.RecipeIngredientRepository; // NEW
import com.toastedsiopao.repository.UnitOfMeasureRepository;
import org.slf4j.Logger; // NEW
import org.slf4j.LoggerFactory; // NEW
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InventoryItemServiceImpl implements InventoryItemService {

	// --- NEW ---
	private static final Logger log = LoggerFactory.getLogger(InventoryItemServiceImpl.class);

	@Autowired
	private InventoryItemRepository itemRepository;
	@Autowired
	private InventoryCategoryRepository categoryRepository;
	@Autowired
	private UnitOfMeasureRepository unitRepository;

	// --- NEWLY INJECTED ---
	@Autowired
	private RecipeIngredientRepository recipeIngredientRepository;

	@Override
	@Transactional(readOnly = true)
	public List<InventoryItem> findAll() {
		return itemRepository.findAll(); // Consider sorting
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<InventoryItem> findById(Long id) {
		return itemRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<InventoryItem> findByName(String name) {
		return itemRepository.findByNameIgnoreCase(name);
	}

	@Override
	public InventoryItem save(InventoryItemDto itemDto) {
		// 1. Fetch related entities
		InventoryCategory category = categoryRepository.findById(itemDto.getCategoryId()).orElseThrow(
				() -> new RuntimeException("Inventory Category not found with id: " + itemDto.getCategoryId()));
		UnitOfMeasure unit = unitRepository.findById(itemDto.getUnitId())
				.orElseThrow(() -> new RuntimeException("Unit of Measure not found with id: " + itemDto.getUnitId()));

		// 2. Check for duplicate name (only if creating new or changing name)
		Optional<InventoryItem> existingItemOpt = itemRepository.findByNameIgnoreCase(itemDto.getName().trim());
		if (existingItemOpt.isPresent()) {
			InventoryItem existingItem = existingItemOpt.get();
			// Allow saving if it's the same item being updated, otherwise throw error
			if (itemDto.getId() == null || !existingItem.getId().equals(itemDto.getId())) {
				throw new IllegalArgumentException(
						"Inventory item name '" + itemDto.getName().trim() + "' already exists.");
			}
		}

		// 3. Ensure critical threshold <= low threshold
		if (itemDto.getCriticalStockThreshold().compareTo(itemDto.getLowStockThreshold()) > 0) {
			throw new IllegalArgumentException("Critical stock threshold cannot be greater than low stock threshold.");
		}

		// 4. Create or Update Entity
		InventoryItem item;
		if (itemDto.getId() != null) {
			item = itemRepository.findById(itemDto.getId())
					.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + itemDto.getId()));
		} else {
			item = new InventoryItem();
		}

		// 5. Map DTO to Entity
		item.setName(itemDto.getName().trim());
		item.setCategory(category);
		item.setUnit(unit);
		item.setCurrentStock(itemDto.getCurrentStock());
		item.setLowStockThreshold(itemDto.getLowStockThreshold());
		item.setCriticalStockThreshold(itemDto.getCriticalStockThreshold());
		item.setCostPerUnit(itemDto.getCostPerUnit());

		return itemRepository.save(item);
	}

	// --- MODIFIED: deleteById method ---
	@Override
	public void deleteById(Long id) {
		InventoryItem item = itemRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + id));

		// Check if this item is used in any recipes
		List<RecipeIngredient> recipes = recipeIngredientRepository.findByInventoryItem(item);
		if (!recipes.isEmpty()) {
			throw new RuntimeException("Cannot delete item. It is used in " + recipes.size() + " product recipe(s).");
		}

		itemRepository.deleteById(id);
	}
	// --- END MODIFIED ---

	@Override
	@Transactional(readOnly = true)
	public List<InventoryItem> searchItems(String keyword, Long categoryId) {
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasCategory = categoryId != null;

		if (hasKeyword && hasCategory) {
			InventoryCategory category = categoryRepository.findById(categoryId)
					.orElseThrow(() -> new RuntimeException("Inventory Category not found with id: " + categoryId));
			return itemRepository.findByNameContainingIgnoreCaseAndCategoryOrderByNameAsc(keyword.trim(), category);
		} else if (hasKeyword) {
			return itemRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim());
		} else if (hasCategory) {
			InventoryCategory category = categoryRepository.findById(categoryId)
					.orElseThrow(() -> new RuntimeException("Inventory Category not found with id: " + categoryId));
			return itemRepository.findByCategoryOrderByNameAsc(category);
		} else {
			return itemRepository.findAll(); // Consider sorting
		}
	}

	// --- Stock Report Methods ---
	@Override
	@Transactional(readOnly = true)
	public List<InventoryItem> findLowStockItems() {
		return itemRepository.findLowStockItems();
	}

	@Override
	@Transactional(readOnly = true)
	public List<InventoryItem> findCriticalStockItems() {
		return itemRepository.findCriticalStockItems();
	}

	@Override
	@Transactional(readOnly = true)
	public List<InventoryItem> findOutOfStockItems() {
		return itemRepository.findOutOfStockItems();
	}

	// --- NEW: Implemented adjustStock method ---
	@Override
	public InventoryItem adjustStock(Long itemId, BigDecimal quantityChange, String reason) {
		InventoryItem item = itemRepository.findById(itemId)
				.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + itemId));

		BigDecimal newStock = item.getCurrentStock().add(quantityChange);
		if (newStock.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Stock cannot go below zero. Current stock: " + item.getCurrentStock()
					+ ", Change: " + quantityChange);
		}

		item.setCurrentStock(newStock);
		InventoryItem savedItem = itemRepository.save(item);

		log.info("Stock adjusted for Inventory ID {}: Change={}, New Stock={}, Reason={}", itemId, quantityChange,
				newStock, reason);

		return savedItem;
	}
	// --- END NEW ---
}