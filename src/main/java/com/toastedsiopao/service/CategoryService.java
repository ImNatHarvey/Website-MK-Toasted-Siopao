package com.toastedsiopao.service;

import com.toastedsiopao.dto.CategoryDto; 
import com.toastedsiopao.model.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
	List<Category> findAll();

	Optional<Category> findById(Long id);

	Category save(Category category); 

	Category saveFromDto(CategoryDto categoryDto);

	Category updateFromDto(CategoryDto categoryDto);

	void deleteById(Long id);

	Optional<Category> findByName(String name); 
}