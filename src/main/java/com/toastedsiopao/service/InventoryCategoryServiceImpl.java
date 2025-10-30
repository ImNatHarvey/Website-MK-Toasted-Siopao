package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryCategoryDto; // Import DTO
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.repository.InventoryCategoryRepository;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
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

	// Centralized validation for name uniqueness
	private void validateNameUniqueness(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		if (repository.findByNameIgnoreCase(name.trim()).isPresent()) {
			throw new IllegalArgumentException("Inventory category name '" + name.trim() + "' already exists.");
		}
	}

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

	@Override
	public void deleteById(Long id) {
		// Existing check done in controller is sufficient for now
		// (Prevent deletion if category is in use by items)
		if (!repository.existsById(id)) {
			log.warn("Attempted to delete non-existent inventory category with ID: {}", id);
			return; // Or throw exception
		}
		repository.deleteById(id);
		log.info("Deleted inventory category with ID: {}", id);
	}
}