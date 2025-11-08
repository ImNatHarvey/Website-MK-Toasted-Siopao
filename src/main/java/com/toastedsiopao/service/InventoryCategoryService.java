package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryCategoryDto; 
import com.toastedsiopao.model.InventoryCategory;
import java.util.List;
import java.util.Optional;

public interface InventoryCategoryService {
	List<InventoryCategory> findAll();

	Optional<InventoryCategory> findById(Long id);

	Optional<InventoryCategory> findByName(String name);

	InventoryCategory save(InventoryCategory category); 

	InventoryCategory saveFromDto(InventoryCategoryDto categoryDto);
	
	InventoryCategory updateFromDto(InventoryCategoryDto categoryDto);

	void deleteById(Long id);
}