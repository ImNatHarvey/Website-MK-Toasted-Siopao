package com.toastedsiopao.service;

import com.toastedsiopao.model.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
	List<Category> findAll();

	Optional<Category> findById(Long id);

	Category save(Category category); // Can be used for create and update

	void deleteById(Long id);

	Optional<Category> findByName(String name); // Check for duplicates
}
