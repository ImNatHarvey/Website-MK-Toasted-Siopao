package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryItemDto;
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem;
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
import java.time.LocalDate;
import java.time.LocalDateTime; // Added
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	public List<InventoryItem> findAllActive() {
		return itemRepository.findAllActiveByOrderByNameAsc();
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
		LocalDate anchorDateForExpiration = LocalDate.now();
		Integer oldExpirationDays = null;

		boolean isNew = itemDto.getId() == null;
		if (!isNew) {
			item = itemRepository.findById(itemDto.getId())
					.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + itemDto.getId()));

			// Capture state before update
			oldExpirationDays = item.getExpirationDays();
			if (item.getLastUpdated() != null) {
				anchorDateForExpiration = item.getLastUpdated().toLocalDate();
			}

			if ("INACTIVE".equals(itemDto.getItemStatus()) && "ACTIVE".equals(item.getItemStatus())) {
				if (item.getCurrentStock().compareTo(BigDecimal.ZERO) > 0) {
					throw new IllegalArgumentException(
							"status.hasStock:• Cannot deactivate '" + item.getName() + "'. Item still has "
									+ item.getCurrentStock() + " in stock. Please adjust stock to 0 first.");
				}
				if (recipeIngredientRepository.countByInventoryItem(item) > 0) {
					throw new IllegalArgumentException("status.inRecipe:• Cannot deactivate '" + item.getName()
							+ "'. Item is currently used in one or more product recipes.");
				}
			}
		} else {
			item = new InventoryItem();
			item.setCurrentStock(Optional.ofNullable(itemDto.getCurrentStock()).orElse(BigDecimal.ZERO));
		}

		item.setName(itemDto.getName().trim());
		item.setCategory(category);
		item.setUnit(unit);
		item.setItemStatus(Optional.ofNullable(itemDto.getItemStatus()).orElse("ACTIVE"));
		item.setLowStockThreshold(itemDto.getLowStockThreshold());
		item.setCriticalStockThreshold(itemDto.getCriticalStockThreshold());
		item.setCostPerUnit(itemDto.getCostPerUnit());

		// Update Expiration Days setting
		Integer newExpirationDays = itemDto.getExpirationDays();
		item.setExpirationDays(newExpirationDays);

		// --- DATE LOGIC ---
		// 1. Update Received Date (Only if explicitly changed in DTO, or defaults for
		// new item)
		// This step is completely independent of Expiration Date now.
		if (itemDto.getReceivedDate() != null) {
			item.setReceivedDate(itemDto.getReceivedDate());
		} else if (isNew) {
			item.setReceivedDate(LocalDate.now());
		} else if (item.getReceivedDate() == null) {
			// Fallback for legacy data
			item.setReceivedDate(LocalDate.now());
		}

		// 2. Update Expiration Date
		// Logic: Only recalculate if the shelf life (days) setting actually changed.
		// Anchor: Use the 'Last Updated' date (Dec 02), not the 'Received Date' (Nov
		// 20).
		boolean daysChanged = (newExpirationDays != null && !newExpirationDays.equals(oldExpirationDays))
				|| (newExpirationDays == null && oldExpirationDays != null);

		if (daysChanged || isNew) {
			if (newExpirationDays != null && newExpirationDays > 0) {
				item.setExpirationDate(anchorDateForExpiration.plusDays(newExpirationDays));
			} else {
				item.setExpirationDate(null);
			}
		}
		// If days didn't change, we touch nothing. Even if ReceivedDate changed above,
		// ExpirationDate stays fixed.
		// --- END DATE LOGIC ---

		try {
			InventoryItem savedItem = itemRepository.save(item);
			log.info("{} inventory item: ID={}, Name='{}', Status='{}'", isNew ? "Created" : "Updated",
					savedItem.getId(), savedItem.getName(), savedItem.getItemStatus());
			return savedItem;
		} catch (Exception e) {
			log.error("Database error saving inventory item '{}': {}", itemDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not save inventory item due to a database error.", e);
		}
	}

	@Override
	public void deactivateItem(Long id) {
		InventoryItem item = itemRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + id));

		if (item.getCurrentStock().compareTo(BigDecimal.ZERO) > 0) {
			throw new IllegalArgumentException("status.hasStock:• Item '" + item.getName() + "' still has "
					+ item.getCurrentStock() + " stock. Cannot deactivate.");
		}
		if (recipeIngredientRepository.countByInventoryItem(item) > 0) {
			throw new IllegalArgumentException("status.inRecipe:• Cannot deactivate '" + item.getName()
					+ "'. Item is currently used in one or more product recipes.");
		}

		item.setItemStatus("INACTIVE");
		itemRepository.save(item);
		log.info("Deactivated inventory item: ID={}, Name='{}'", id, item.getName());
	}

	@Override
	public void activateItem(Long id) {
		InventoryItem item = itemRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + id));
		item.setItemStatus("ACTIVE");
		itemRepository.save(item);
		log.info("Activated inventory item: ID={}, Name='{}'", id, item.getName());
	}

	@Override
	public void deleteItem(Long id) {
		InventoryItem item = itemRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + id));

		if (item.getCurrentStock().compareTo(BigDecimal.ZERO) > 0) {
			throw new IllegalArgumentException(
					"• Item '" + item.getName() + "' still has " + item.getCurrentStock() + " stock. Cannot delete.");
		}

		if (recipeIngredientRepository.countByInventoryItem(item) > 0) {
			throw new IllegalArgumentException("• Item '" + item.getName()
					+ "' is used in one or more product recipes and cannot be permanently deleted. Please deactivate it via the Edit menu.");
		}

		itemRepository.delete(item);
		log.info("Permanently deleted inventory item: ID={}, Name='{}'", id, item.getName());
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
	public InventoryItem adjustStock(Long itemId, BigDecimal quantityChange, String reason, LocalDate receivedDate,
			Integer expirationDays) {
		InventoryItem item = itemRepository.findByIdForUpdate(itemId)
				.orElseThrow(() -> new RuntimeException("Inventory Item not found with id: " + itemId));

		if (!"ACTIVE".equals(item.getItemStatus())) {
			throw new IllegalArgumentException("Cannot adjust stock for an INACTIVE item: " + item.getName());
		}

		BigDecimal newStock = item.getCurrentStock().add(quantityChange);
		if (newStock.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Stock cannot go below zero. Current stock: " + item.getCurrentStock()
					+ ", Change: " + quantityChange);
		}

		item.setCurrentStock(newStock);

		if (quantityChange.compareTo(BigDecimal.ZERO) > 0) {
			// 1. Recalculate Expiration Date relative to TODAY (LocalDate.now())
			// This ensures the expiration reflects the new stock being added now.
			if (expirationDays != null && expirationDays > 0) {
				item.setExpirationDate(LocalDate.now().plusDays(expirationDays));
			} else {
				// If explicit 0/null expiration days provided (non-perishable), clear the date
				if (expirationDays != null) {
					item.setExpirationDate(null);
				}
			}

			// 2. Only update Received Date if the user explicitly provided one.
			if (receivedDate != null) {
				item.setReceivedDate(receivedDate);
			}
		}

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
	public long countByUnit(UnitOfMeasure unit) {
		return itemRepository.countByUnit(unit);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getInventoryMetrics(String keyword, Long categoryId) {
		Map<String, Object> metrics = new HashMap<>();

		InventoryCategory category = null;
		if (categoryId != null) {
			category = categoryRepository.findById(categoryId).orElse(null);
		}
		String parsedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

		long totalItems = itemRepository.countFilteredItems(parsedKeyword, category);
		BigDecimal totalValue = itemRepository.sumFilteredStockValue(parsedKeyword, category);
		long lowStock = itemRepository.countFilteredLowStock(parsedKeyword, category);
		long criticalStock = itemRepository.countFilteredCriticalStock(parsedKeyword, category);
		long outOfStock = itemRepository.countFilteredOutOfStock(parsedKeyword, category);

		metrics.put("totalItems", totalItems);
		metrics.put("totalValue", totalValue);
		metrics.put("lowStock", lowStock);
		metrics.put("criticalStock", criticalStock);
		metrics.put("outOfStock", outOfStock);

		return metrics;
	}
}