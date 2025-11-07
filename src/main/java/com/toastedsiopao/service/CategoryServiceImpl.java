package com.toastedsiopao.service;

import com.toastedsiopao.dto.CategoryDto; // Import DTO
import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product; // NEW IMPORT
import com.toastedsiopao.repository.CategoryRepository;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy; // NEW IMPORT
import org.springframework.data.domain.Page; // NEW IMPORT
import org.springframework.data.domain.PageRequest; // NEW IMPORT
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

	// NEW: Inject ProductService
	// Use @Lazy to break circular dependency (CategoryService -> ProductService ->
	// CategoryService)
	@Autowired
	@Lazy
	private ProductService productService;

	// Centralized validation for name uniqueness
	private void validateNameUniqueness(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		if (categoryRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
			throw new IllegalArgumentException("Product category name '" + name.trim() + "' already exists.");
		}
	}

	// --- NEW: Centralized validation for name uniqueness on update ---
	private void validateNameUniquenessOnUpdate(String name, Long categoryId) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		Optional<Category> existing = categoryRepository.findByNameIgnoreCase(name.trim());
		if (existing.isPresent() && !existing.get().getId().equals(categoryId)) {
			throw new IllegalArgumentException("Product category name '" + name.trim() + "' already exists.");
		}
	}
	// --- END NEW ---

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

	// --- NEW: Update method using DTO with validation ---
	@Override
	public Category updateFromDto(CategoryDto categoryDto) {
		if (categoryDto == null || categoryDto.getId() == null) {
			throw new IllegalArgumentException("Category data or ID cannot be null for update.");
		}

		// 1. Find the existing category
		Category categoryToUpdate = categoryRepository.findById(categoryDto.getId())
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryDto.getId()));

		// 2. Validate the new name
		validateNameUniquenessOnUpdate(categoryDto.getName(), categoryDto.getId());

		// 3. Update the name
		categoryToUpdate.setName(categoryDto.getName().trim());

		try {
			// 4. Save
			Category savedCategory = categoryRepository.save(categoryToUpdate);
			log.info("Updated product category: ID={}, Name='{}'", savedCategory.getId(), savedCategory.getName());
			return savedCategory;
		} catch (Exception e) {
			log.error("Database error updating product category '{}': {}", categoryDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not update product category due to a database error.", e);
		}
	}
	// --- End NEW Method ---

	// **** METHOD UPDATED ****
	@Override
	public void deleteById(Long id) {
		// 1. Find the category first
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

		// 2. Check if any products are using this category
		Page<Product> productsInCategory = productService.findByCategory(id, PageRequest.of(0, 1));
		if (!productsInCategory.isEmpty()) {
			throw new RuntimeException("Cannot delete '" + category.getName() + "'. Associated with "
					+ productsInCategory.getTotalElements() + " product(s).");
		}

		// 3. If no products, proceed with deletion
		categoryRepository.deleteById(id);
		log.info("Deleted product category with ID: {}", id);
	}
	// **** END OF UPDATED METHOD ****

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findByName(String name) {
		if (!StringUtils.hasText(name))
			return Optional.empty();
		return categoryRepository.findByNameIgnoreCase(name.trim());
	}
}