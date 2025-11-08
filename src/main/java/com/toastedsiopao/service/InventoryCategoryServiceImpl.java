package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryCategoryDto;
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.repository.InventoryCategoryRepository;
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; 
import org.springframework.data.domain.PageRequest; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; 

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InventoryCategoryServiceImpl implements InventoryCategoryService {

	private static final Logger log = LoggerFactory.getLogger(InventoryCategoryServiceImpl.class);

	@Autowired
	private InventoryCategoryRepository repository;

	@Autowired
	private InventoryItemService inventoryItemService;

	private void validateNameUniqueness(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		if (repository.findByNameIgnoreCase(name.trim()).isPresent()) {
			throw new IllegalArgumentException("Inventory category name '" + name.trim() + "' already exists.");
		}
	}

	private void validateNameUniquenessOnUpdate(String name, Long categoryId) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name cannot be blank.");
		}
		Optional<InventoryCategory> existing = repository.findByNameIgnoreCase(name.trim());
		if (existing.isPresent() && !existing.get().getId().equals(categoryId)) {
			throw new IllegalArgumentException("Inventory category name '" + name.trim() + "' already exists.");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<InventoryCategory> findAll() {
		return repository.findAll(); 
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
		if (category == null || !StringUtils.hasText(category.getName())) {
			throw new IllegalArgumentException("Cannot save category with null or blank name.");
		}
		return repository.save(category);
	}

	@Override
	public InventoryCategory saveFromDto(InventoryCategoryDto categoryDto) {
		if (categoryDto == null) {
			throw new IllegalArgumentException("Category data cannot be null.");
		}
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
	
	@Override
	public InventoryCategory updateFromDto(InventoryCategoryDto categoryDto) {
		if (categoryDto == null || categoryDto.getId() == null) {
			throw new IllegalArgumentException("Category data or ID cannot be null for update.");
		}

		InventoryCategory categoryToUpdate = repository.findById(categoryDto.getId()).orElseThrow(
				() -> new RuntimeException("Inventory Category not found with id: " + categoryDto.getId()));

		validateNameUniquenessOnUpdate(categoryDto.getName(), categoryDto.getId());

		categoryToUpdate.setName(categoryDto.getName().trim());

		try {
			InventoryCategory savedCategory = repository.save(categoryToUpdate);
			log.info("Updated inventory category: ID={}, Name='{}'", savedCategory.getId(), savedCategory.getName());
			return savedCategory;
		} catch (Exception e) {
			log.error("Database error updating inventory category '{}': {}", categoryDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not update inventory category due to a database error.", e);
		}
	}
	
	@Override
	public void deleteById(Long id) {
		
		InventoryCategory category = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

		Page<InventoryItem> itemsInCategory = inventoryItemService.searchItems(null, id, PageRequest.of(0, 1));
		if (!itemsInCategory.isEmpty()) {
			throw new RuntimeException("Cannot delete category '" + category.getName() + "'. It is associated with "
					+ itemsInCategory.getTotalElements() + " inventory item(s).");
		}

		repository.deleteById(id);
		log.info("Deleted inventory category with ID: {}", id);
	}
}