package com.toastedsiopao.service;

import com.toastedsiopao.model.Category;
import com.toastedsiopao.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.List;
import java.util.Optional;

@Service
@Transactional // Good practice for service layer methods modifying data
public class CategoryServiceImpl implements CategoryService {

	@Autowired
	private CategoryRepository categoryRepository;

	@Override
	@Transactional(readOnly = true) // readOnly optimization for find methods
	public List<Category> findAll() {
		return categoryRepository.findAll();
		// Consider adding sorting later, e.g., findAll(Sort.by("name"))
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findById(Long id) {
		return categoryRepository.findById(id);
	}

	@Override
	public Category save(Category category) {
		// Add duplicate name check if needed before saving
		return categoryRepository.save(category);
	}

	@Override
	public void deleteById(Long id) {
		// Add checks if category exists or is deletable before deleting
		categoryRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findByName(String name) {
		return categoryRepository.findByNameIgnoreCase(name);
	}
}
