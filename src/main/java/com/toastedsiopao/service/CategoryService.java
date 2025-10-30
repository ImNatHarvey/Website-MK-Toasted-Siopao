package com.toastedsiopao.service;

import com.toastedsiopao.dto.CategoryDto; // **** ADDED IMPORT ****
import com.toastedsiopao.model.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
	List<Category> findAll();

	Optional<Category> findById(Long id);

	Category save(Category category); // Can be used for create and update

	// **** ADDED METHOD SIGNATURE ****
	/**
	 * Saves a new Category from a DTO, including validation.
	 * 
	 * @param categoryDto The DTO containing category data.
	 * @return The saved Category entity.
	 * @throws IllegalArgumentException if validation fails (e.g., duplicate name).
	 */
	Category saveFromDto(CategoryDto categoryDto);
	// **** END ADDED METHOD ****

	void deleteById(Long id);

	Optional<Category> findByName(String name); // Check for duplicates
}