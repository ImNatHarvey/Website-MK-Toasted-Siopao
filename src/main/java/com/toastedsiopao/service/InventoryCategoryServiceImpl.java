package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryCategoryDto; // Import DTO
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem; // NEW IMPORT
import com.toastedsiopao.repository.InventoryCategoryRepository;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // NEW IMPORT
import org.springframework.data.domain.PageRequest; // NEW IMPORT
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Import StringUtils

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InventoryCategoryServiceImpl implements InventoryCategoryService {

	// --- Add Logger ---
	private static final Logger log = LoggerFactory.getLogger(InventoryCategoryServiceImpl.class);

	@Autowired
	private InventoryCategoryRepository repository;

	// NEW: Inject InventoryItemService
	@Autowired
	private InventoryItemService inventoryItemService;

	// Centralized validation for name uniqueness
	private void validateNameUniqueness(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		if (repository.findByNameIgnoreCase(name.trim()).isPresent()) {
			throw new IllegalArgumentException("Inventory category name '" + name.trim() + "' already exists.");
		}
	}

	// --- NEW: Centralized validation for name uniqueness on update ---
	private void validateNameUniquenessOnUpdate(String name, Long categoryId) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		Optional<InventoryCategory> existing = repository.findByNameIgnoreCase(name.trim());
		if (existing.isPresent() && !existing.get().getId().equals(categoryId)) {
			throw new IllegalArgumentException("Inventory category name '" + name.trim() + "' already exists.");
		}
	}
	// --- END NEW ---

	@Override
	@Transactional(readOnly = true)
	public List<InventoryCategory> findAll() {
		return repository.findAll(); // Consider adding sorting later
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<InventoryCategory> findById(Long id) {
		return repository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<InventoryCategory> findByName(String name) {
		if (!StringUtils.hasText(name))
			return Optional.empty();
		return repository.findByNameIgnoreCase(name.trim());
	}

	@Override
	public InventoryCategory save(InventoryCategory category) {
		// Basic validation before saving directly (used internally or if needed)
		if (category == null || !StringUtils.hasText(category.getName())) {
			throw new IllegalArgumentException("Cannot save category with null or blank name.");
		}
		// Note: This basic save doesn't re-check for duplicates if called directly
		return repository.save(category);
	}

	// --- NEW: Save method using DTO with validation ---
	@Override
	public InventoryCategory saveFromDto(InventoryCategoryDto categoryDto) {
		if (categoryDto == null) {
			throw new IllegalArgumentException("Category data cannot be null.");
		}
		// Validate name uniqueness before proceeding
		validateNameUniqueness(categoryDto.getName());

		InventoryCategory newCategory = new InventoryCategory(categoryDto.getName().trim());
		try {
			InventoryCategory savedCategory = repository.save(newCategory);
			log.info("Saved new inventory category: ID={}, Name='{}'", savedCategory.getId(), savedCategory.getName());
			return savedCategory;
		} catch (Exception e) {
			log.error("Database error saving inventory category '{}': {}", categoryDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not save inventory category due to a database error.", e);
		}
	}
	// --- End NEW Method ---

	// --- NEW: Update method using DTO with validation ---
	@Override
	public InventoryCategory updateFromDto(InventoryCategoryDto categoryDto) {
		if (categoryDto == null || categoryDto.getId() == null) {
			throw new IllegalArgumentException("Category data or ID cannot be null for update.");
		}

		// 1. Find the existing category
		InventoryCategory categoryToUpdate = repository.findById(categoryDto.getId()).orElseThrow(
				() -> new RuntimeException("Inventory Category not found with id: " + categoryDto.getId()));

		// 2. Validate the new name
		validateNameUniquenessOnUpdate(categoryDto.getName(), categoryDto.getId());

		// 3. Update the name
		categoryToUpdate.setName(categoryDto.getName().trim());

		try {
			// 4. Save
			InventoryCategory savedCategory = repository.save(categoryToUpdate);
			log.info("Updated inventory category: ID={}, Name='{}'", savedCategory.getId(), savedCategory.getName());
			return savedCategory;
		} catch (Exception e) {
			log.error("Database error updating inventory category '{}': {}", categoryDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not update inventory category due to a database error.", e);
		}
	}
	// --- End NEW Method ---

	// **** METHOD UPDATED ****
	@Override
	public void deleteById(Long id) {
		// 1. Find the category first
		InventoryCategory category = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

		// 2. Check if any items are using this category
		Page<InventoryItem> itemsInCategory = inventoryItemService.searchItems(null, id, PageRequest.of(0, 1));
		if (!itemsInCategory.isEmpty()) {
			throw new RuntimeException("Cannot delete category '" + category.getName() + "'. It is associated with "
					+ itemsInCategory.getTotalElements() + " inventory item(s).");
		}

		// 3. If no items, proceed with deletion
		repository.deleteById(id);
		log.info("Deleted inventory category with ID: {}", id);
	}
	// **** END OF UPDATED METHOD ****
}