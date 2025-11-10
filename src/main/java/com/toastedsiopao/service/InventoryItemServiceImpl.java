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

	private void validateThresholds(BigDecimal lowThreshold, BigDecimal criticalThreshold) {

		if (lowThreshold == null || lowThreshold.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Low stock threshold must be greater than 0.");
		}
		if (criticalThreshold == null || criticalThreshold.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Critical stock threshold must be greater than 0.");
		}

		if (criticalThreshold.compareTo(lowThreshold) > 0) {
			throw new IllegalArgumentException("Critical stock threshold cannot be greater than low stock threshold.");
		}
	}

	private void validateNameUniqueness(String name, Long currentItemId) {
		Optional<InventoryItem> existingItemOpt = itemRepository.findByNameIgnoreCase(name.trim());
		if (existingItemOpt.isPresent()) {
			InventoryItem existingItem = existingItemOpt.get();
			if (currentItemId == null || !existingItem.getId().equals(currentItemId)) {
				throw new IllegalArgumentException("Inventory item name '" + name.trim() + "' already exists.");
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<InventoryItem> findAll() {
		return itemRepository.findAllByOrderByNameAsc();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<InventoryItem> findAll(Pageable pageable) {
		return itemRepository.findAll(pageable);
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
		// --- Input Validation ---
		if (itemDto == null) {
			throw new IllegalArgumentException("Inventory item data cannot be null.");
		}
		if (!StringUtils.hasText(itemDto.getName())) {
			throw new IllegalArgumentException("Item name cannot be blank.");
		}
		if (itemDto.getCategoryId() == null) {
			throw new IllegalArgumentException("Category must be selected.");
		}
		if (itemDto.getUnitId() == null) {
			throw new IllegalArgumentException("Unit must be selected.");
		}
		if (itemDto.getCurrentStock() == null)
			itemDto.setCurrentStock(BigDecimal.ZERO);

		validateThresholds(itemDto.getLowStockThreshold(), itemDto.getCriticalStockThreshold());
		validateNameUniqueness(itemDto.getName(), itemDto.getId());

		InventoryCategory category = categoryRepository.findById(itemDto.getCategoryId()).orElseThrow(
				() -> new RuntimeException("Inventory Category not found with id: " + itemDto.getCategoryId()));
		UnitOfMeasure unit = unitRepository.findById(itemDto.getUnitId())
				.orElseThrow(() -> new RuntimeException("Unit of Measure not found with id: " + itemDto.getUnitId()));

		InventoryItem item;
		boolean isNew = itemDto.getId() == null;
		if (!isNew) {
			item = itemRepository.findById(itemDto.getId())
					.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + itemDto.getId()));
		} else {
			item = new InventoryItem();
		}

		item.setName(itemDto.getName().trim());
		item.setCategory(category);
		item.setUnit(unit);

		if (isNew) {
			item.setCurrentStock(Optional.ofNullable(itemDto.getCurrentStock()).orElse(BigDecimal.ZERO));
		}

		item.setLowStockThreshold(itemDto.getLowStockThreshold());
		item.setCriticalStockThreshold(itemDto.getCriticalStockThreshold());
		item.setCostPerUnit(itemDto.getCostPerUnit());

		try {
			InventoryItem savedItem = itemRepository.save(item);
			log.info("{} inventory item: ID={}, Name='{}'", isNew ? "Created" : "Updated", savedItem.getId(),
					savedItem.getName());
			return savedItem;
		} catch (Exception e) {
			log.error("Database error saving inventory item '{}': {}", itemDto.getName(), e.getMessage(), e);

			throw new RuntimeException("Could not save inventory item due to a database error.", e);
		}
	}

	@Override
	public void deleteById(Long id) {
		// --- MODIFIED: Removed manual pre-check ---
		InventoryItem item = itemRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + id));

		// Let the database throw DataIntegrityViolationException if relations exist
		// (e.g., in RecipeIngredient)
		itemRepository.deleteById(id);

		log.info("Deleted inventory item: ID={}, Name='{}'", id, item.getName());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<InventoryItem> searchItems(String keyword, Long categoryId, Pageable pageable) {
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasCategory = categoryId != null;

		if (hasKeyword && hasCategory) {
			InventoryCategory category = categoryRepository.findById(categoryId)
					.orElseThrow(() -> new RuntimeException("Inventory Category not found with id: " + categoryId));
			return itemRepository.findByNameContainingIgnoreCaseAndCategoryOrderByNameAsc(keyword.trim(), category,
					pageable);
		} else if (hasKeyword) {
			return itemRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim(), pageable);
		} else if (hasCategory) {
			InventoryCategory category = categoryRepository.findById(categoryId)
					.orElseThrow(() -> new RuntimeException("Inventory Category not found with id: " + categoryId));
			return itemRepository.findByCategoryOrderByNameAsc(category, pageable);
		} else {
			return itemRepository.findAll(pageable);
		}
	}

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

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getTotalStockQuantity() {
		return itemRepository.sumTotalStockQuantity();
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getTotalStockValue() {
		return itemRepository.sumTotalStockValue();
	}

	@Override
	@Transactional(readOnly = true)
	public long countLowStockItems() {
		return itemRepository.countLowStockItems();
	}

	@Override
	@Transactional(readOnly = true)
	public long countCriticalStockItems() {
		return itemRepository.countCriticalStockItems();
	}

	@Override
	@Transactional(readOnly = true)
	public long countOutOfStockItems() {
		return itemRepository.countOutOfStockItems();
	}

	@Override
	@Transactional(readOnly = true)
	public long countByUnit(UnitOfMeasure unit) {
		return itemRepository.countByUnit(unit);
	}
}