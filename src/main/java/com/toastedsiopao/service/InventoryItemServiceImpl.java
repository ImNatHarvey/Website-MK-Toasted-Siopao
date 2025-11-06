package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryItemDto;
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.RecipeIngredient;
import com.toastedsiopao.model.UnitOfMeasure;
import com.toastedsiopao.repository.InventoryCategoryRepository;
import com.toastedsiopao.repository.InventoryItemRepository;
import com.toastedsiopao.repository.RecipeIngredientRepository;
import com.toastedsiopao.repository.UnitOfMeasureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryItemServiceImpl implements InventoryItemService {

	private static final Logger log = LoggerFactory.getLogger(InventoryItemServiceImpl.class);

	@Autowired
	private InventoryItemRepository itemRepository;
	@Autowired
	private InventoryCategoryRepository categoryRepository;
	@Autowired
	private UnitOfMeasureRepository unitRepository;
	@Autowired
	private RecipeIngredientRepository recipeIngredientRepository;

	// --- Centralized Validation Methods ---
	private void validateThresholds(BigDecimal lowThreshold, BigDecimal criticalThreshold) {
		if (criticalThreshold != null && lowThreshold != null && criticalThreshold.compareTo(lowThreshold) > 0) {
			throw new IllegalArgumentException("Critical stock threshold cannot be greater than low stock threshold.");
		}
	}

	private void validateNameUniqueness(String name, Long currentItemId) {
		Optional<InventoryItem> existingItemOpt = itemRepository.findByNameIgnoreCase(name.trim());
		if (existingItemOpt.isPresent()) {
			InventoryItem existingItem = existingItemOpt.get();
			// Allow saving if it's the same item being updated, otherwise throw error
			if (currentItemId == null || !existingItem.getId().equals(currentItemId)) {
				throw new IllegalArgumentException("Inventory item name '" + name.trim() + "' already exists.");
			}
		}
	}
	// --- End Validation Methods ---

	@Override
	@Transactional(readOnly = true)
	public List<InventoryItem> findAll() {
		return itemRepository.findAllByOrderByNameAsc(); // UPDATED
	}

	// --- NEW ---
	@Override
	@Transactional(readOnly = true)
	public Page<InventoryItem> findAll(Pageable pageable) {
		return itemRepository.findAll(pageable);
	}
	// --- END NEW ---

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
		// --- Input Validation ---
		if (itemDto == null) {
			throw new IllegalArgumentException("Inventory item data cannot be null.");
		}
		if (!StringUtils.hasText(itemDto.getName())) {
			throw new IllegalArgumentException("Item name cannot be blank."); // Should be caught by @NotBlank but good
																				// practice
		}
		if (itemDto.getCategoryId() == null) {
			throw new IllegalArgumentException("Category must be selected.");
		}
		if (itemDto.getUnitId() == null) {
			throw new IllegalArgumentException("Unit must be selected.");
		}
		// Basic null checks for numbers (should be caught by @NotNull, but defense)
		if (itemDto.getCurrentStock() == null)
			itemDto.setCurrentStock(BigDecimal.ZERO);
		if (itemDto.getLowStockThreshold() == null)
			itemDto.setLowStockThreshold(BigDecimal.ZERO);
		if (itemDto.getCriticalStockThreshold() == null)
			itemDto.setCriticalStockThreshold(BigDecimal.ZERO);
		// --- End Input Validation ---

		// --- Moved Validations Here ---
		validateThresholds(itemDto.getLowStockThreshold(), itemDto.getCriticalStockThreshold());
		validateNameUniqueness(itemDto.getName(), itemDto.getId());
		// --- End Moved Validations ---

		// 1. Fetch related entities (Throw exception if not found)
		InventoryCategory category = categoryRepository.findById(itemDto.getCategoryId()).orElseThrow(
				() -> new RuntimeException("Inventory Category not found with id: " + itemDto.getCategoryId()));
		UnitOfMeasure unit = unitRepository.findById(itemDto.getUnitId())
				.orElseThrow(() -> new RuntimeException("Unit of Measure not found with id: " + itemDto.getUnitId()));

		// 2. Create or Update Entity
		InventoryItem item;
		boolean isNew = itemDto.getId() == null;
		if (!isNew) {
			item = itemRepository.findById(itemDto.getId())
					.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + itemDto.getId()));
		} else {
			item = new InventoryItem();
		}

		// 3. Map DTO to Entity
		item.setName(itemDto.getName().trim()); // Trim name
		item.setCategory(category);
		item.setUnit(unit);
		// Ensure non-null BigDecimal values before setting
		item.setCurrentStock(Optional.ofNullable(itemDto.getCurrentStock()).orElse(BigDecimal.ZERO));
		item.setLowStockThreshold(Optional.ofNullable(itemDto.getLowStockThreshold()).orElse(BigDecimal.ZERO));
		item.setCriticalStockThreshold(
				Optional.ofNullable(itemDto.getCriticalStockThreshold()).orElse(BigDecimal.ZERO));
		item.setCostPerUnit(itemDto.getCostPerUnit()); // Allow null cost

		// Save the item
		try {
			InventoryItem savedItem = itemRepository.save(item);
			log.info("{} inventory item: ID={}, Name='{}'", isNew ? "Created" : "Updated", savedItem.getId(),
					savedItem.getName());
			return savedItem;
		} catch (Exception e) {
			log.error("Database error saving inventory item '{}': {}", itemDto.getName(), e.getMessage(), e);
			// Re-throw a more specific or wrapped exception if needed
			throw new RuntimeException("Could not save inventory item due to a database error.", e);
		}
	}

	@Override
	public void deleteById(Long id) {
		InventoryItem item = itemRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + id));

		// Check if this item is used in any recipes
		List<RecipeIngredient> recipes = recipeIngredientRepository.findByInventoryItem(item);
		if (!recipes.isEmpty()) {
			// Provide more context in the error
			String productNames = recipes.stream()
					.map(r -> r.getProduct() != null ? r.getProduct().getName() : "Unknown Product").distinct().limit(3) // Limit
																															// the
																															// number
																															// of
																															// product
																															// names
																															// listed
					.collect(Collectors.joining(", "));
			if (recipes.size() > 3)
				productNames += ", ...";

			throw new RuntimeException(
					"Cannot delete item '" + item.getName() + "'. It is used in product recipe(s): " + productNames);
		}

		itemRepository.deleteById(id);
		log.info("Deleted inventory item: ID={}, Name='{}'", id, item.getName());
	}

	// --- UPDATED METHOD ---
	@Override
	@Transactional(readOnly = true)
	public Page<InventoryItem> searchItems(String keyword, Long categoryId, Pageable pageable) {
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasCategory = categoryId != null;

		if (hasKeyword && hasCategory) {
			InventoryCategory category = categoryRepository.findById(categoryId)
					.orElseThrow(() -> new RuntimeException("Inventory Category not found with id: " + categoryId)); // <--
																														// FIX
																														// HERE
			return itemRepository.findByNameContainingIgnoreCaseAndCategoryOrderByNameAsc(keyword.trim(), category,
					pageable);
		} else if (hasKeyword) {
			return itemRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim(), pageable);
		} else if (hasCategory) {
			InventoryCategory category = categoryRepository.findById(categoryId)
					.orElseThrow(() -> new RuntimeException("Inventory Category not found with id: " + categoryId));
			return itemRepository.findByCategoryOrderByNameAsc(category, pageable);
		} else {
			return itemRepository.findAll(pageable); // Use the paged findAll
		}
	}
	// --- END UPDATED METHOD ---

	// --- Stock Report Methods (Unchanged) ---
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

	@Override
	public InventoryItem adjustStock(Long itemId, BigDecimal quantityChange, String reason) {
		// Logic unchanged
		InventoryItem item = itemRepository.findById(itemId)
				.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + itemId));

		BigDecimal newStock = item.getCurrentStock().add(quantityChange);
		if (newStock.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Stock cannot go below zero. Current stock: " + item.getCurrentStock()
					+ ", Change: " + quantityChange);
		}

		item.setCurrentStock(newStock);
		InventoryItem savedItem = itemRepository.save(item);

		log.info("Stock adjusted for Inventory ID {}: Change={}, New Stock={}, Reason='{}'", itemId, quantityChange,
				newStock, StringUtils.hasText(reason) ? reason : "No reason provided");

		return savedItem;
	}
}