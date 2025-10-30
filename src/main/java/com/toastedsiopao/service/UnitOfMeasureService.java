package com.toastedsiopao.service;

import com.toastedsiopao.dto.UnitOfMeasureDto; // Import DTO
import com.toastedsiopao.model.UnitOfMeasure;
// Removed InventoryCategoryDto import
import java.util.List;
import java.util.Optional;

public interface UnitOfMeasureService {
	List<UnitOfMeasure> findAll();

	Optional<UnitOfMeasure> findById(Long id);

	Optional<UnitOfMeasure> findByNameOrAbbreviation(String name, String abbreviation);

	UnitOfMeasure save(UnitOfMeasure unit); // Keep direct save if needed internally

	// --- NEW: Save using DTO (includes validation) ---
	UnitOfMeasure saveFromDto(UnitOfMeasureDto unitDto);
	// --- End NEW ---

	void deleteById(Long id);

	// --- REMOVED incorrect method signature ---
	// InventoryCategory saveFromDto(InventoryCategoryDto categoryDto);
}