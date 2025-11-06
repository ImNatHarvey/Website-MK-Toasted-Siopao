package com.toastedsiopao.service;

import com.toastedsiopao.dto.InventoryCategoryDto; // Import DTO
import com.toastedsiopao.model.InventoryCategory;
import java.util.List;
import java.util.Optional;

public interface InventoryCategoryService {
	List<InventoryCategory> findAll();

	Optional<InventoryCategory> findById(Long id);

	Optional<InventoryCategory> findByName(String name);

	InventoryCategory save(InventoryCategory category); // Keep direct save

	// --- NEW: Save using DTO (includes validation) ---
	InventoryCategory saveFromDto(InventoryCategoryDto categoryDto);
	// --- End NEW ---

	// --- NEW: Update using DTO (includes validation) ---
	/**
	 * Updates an existing Inventory Category from a DTO, including validation.
	 *
	 * @param categoryDto The DTO containing category data (must include ID).
	 * @return The updated InventoryCategory entity.
	 * @throws IllegalArgumentException if validation fails (e.g., duplicate name).
	 * @throws RuntimeException         if category is not found.
	 */
	InventoryCategory updateFromDto(InventoryCategoryDto categoryDto);
	// --- End NEW ---

	void deleteById(Long id);
}