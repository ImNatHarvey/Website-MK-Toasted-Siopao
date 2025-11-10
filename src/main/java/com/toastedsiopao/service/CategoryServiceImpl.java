package com.toastedsiopao.service;

import com.toastedsiopao.dto.CategoryDto;
import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

	private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	@Lazy
	private ProductService productService;

	private void validateNameUniqueness(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		if (categoryRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
			throw new IllegalArgumentException("Product category name '" + name.trim() + "' already exists.");
		}
	}

	private void validateNameUniquenessOnUpdate(String name, Long categoryId) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		Optional<Category> existing = categoryRepository.findByNameIgnoreCase(name.trim());
		if (existing.isPresent() && !existing.get().getId().equals(categoryId)) {
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
		if (category == null || !StringUtils.hasText(category.getName())) {
			throw new IllegalArgumentException("Cannot save category with null or blank name.");
		}
		return categoryRepository.save(category);
	}

	@Override
	public Category saveFromDto(CategoryDto categoryDto) {
		if (categoryDto == null) {
			throw new IllegalArgumentException("Category data cannot be null.");
		}
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

	@Override
	public Category updateFromDto(CategoryDto categoryDto) {
		if (categoryDto == null || categoryDto.getId() == null) {
			throw new IllegalArgumentException("Category data or ID cannot be null for update.");
		}
		// 1. Find the existing category
		Category categoryToUpdate = categoryRepository.findById(categoryDto.getId())
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryDto.getId()));

		validateNameUniquenessOnUpdate(categoryDto.getName(), categoryDto.getId());

		categoryToUpdate.setName(categoryDto.getName().trim());

		try {
			Category savedCategory = categoryRepository.save(categoryToUpdate);
			log.info("Updated product category: ID={}, Name='{}'", savedCategory.getId(), savedCategory.getName());
			return savedCategory;
		} catch (Exception e) {
			log.error("Database error updating product category '{}': {}", categoryDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not update product category due to a database error.", e);
		}
	}

	@Override
	public void deleteById(Long id) {

		// --- MODIFIED: Removed manual pre-check ---
		if (!categoryRepository.existsById(id)) {
			throw new RuntimeException("Category not found with id: " + id);
		}

		// Let the database throw DataIntegrityViolationException if relations exist
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