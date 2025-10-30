package com.toastedsiopao.service;

import com.toastedsiopao.dto.CategoryDto; // Import DTO
import com.toastedsiopao.model.Category;
import com.toastedsiopao.repository.CategoryRepository;
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
public class CategoryServiceImpl implements CategoryService {

	// --- Add Logger ---
	private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

	@Autowired
	private CategoryRepository categoryRepository;

	// Centralized validation for name uniqueness
	private void validateNameUniqueness(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		if (categoryRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
			throw new IllegalArgumentException("Product category name '" + name.trim() + "' already exists.");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Category> findAll() {
		return categoryRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findById(Long id) {
		return categoryRepository.findById(id);
	}

	@Override
	public Category save(Category category) {
		// Basic validation before saving directly
		if (category == null || !StringUtils.hasText(category.getName())) {
			throw new IllegalArgumentException("Cannot save category with null or blank name.");
		}
		// Note: This basic save doesn't re-check for duplicates
		return categoryRepository.save(category);
	}

	// --- NEW: Save method using DTO with validation ---
	@Override
	public Category saveFromDto(CategoryDto categoryDto) {
		if (categoryDto == null) {
			throw new IllegalArgumentException("Category data cannot be null.");
		}
		// Validate name uniqueness before proceeding
		validateNameUniqueness(categoryDto.getName());

		Category newCategory = new Category(categoryDto.getName().trim());
		try {
			Category savedCategory = categoryRepository.save(newCategory);
			log.info("Saved new product category: ID={}, Name='{}'", savedCategory.getId(), savedCategory.getName());
			return savedCategory;
		} catch (Exception e) {
			log.error("Database error saving product category '{}': {}", categoryDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not save product category due to a database error.", e);
		}
	}
	// --- End NEW Method ---

	@Override
	public void deleteById(Long id) {
		// Check done in controller is sufficient (prevent deletion if in use)
		if (!categoryRepository.existsById(id)) {
			log.warn("Attempted to delete non-existent product category with ID: {}", id);
			return; // Or throw
		}
		categoryRepository.deleteById(id);
		log.info("Deleted product category with ID: {}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findByName(String name) {
		if (!StringUtils.hasText(name))
			return Optional.empty();
		return categoryRepository.findByNameIgnoreCase(name.trim());
	}
}