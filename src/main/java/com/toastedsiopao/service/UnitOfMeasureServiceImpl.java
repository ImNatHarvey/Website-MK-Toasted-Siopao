package com.toastedsiopao.service;

import com.toastedsiopao.dto.UnitOfMeasureDto; // Import DTO
// REMOVED: import com.toastedsiopao.dto.InventoryCategoryDto;
// REMOVED: import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem; // NEW IMPORT
import com.toastedsiopao.model.UnitOfMeasure;
import com.toastedsiopao.repository.UnitOfMeasureRepository;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Import StringUtils

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // NEW IMPORT

@Service
@Transactional
public class UnitOfMeasureServiceImpl implements UnitOfMeasureService {

	// --- Add Logger ---
	private static final Logger log = LoggerFactory.getLogger(UnitOfMeasureServiceImpl.class);

	@Autowired
	private UnitOfMeasureRepository repository;

	// NEW: Inject InventoryItemService
	@Autowired
	private InventoryItemService inventoryItemService;

	// Centralized validation for name/abbreviation uniqueness
	private void validateUniqueness(String name, String abbreviation) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Unit name cannot be blank.");
		}
		if (!StringUtils.hasText(abbreviation)) {
			throw new IllegalArgumentException("Unit abbreviation cannot be blank.");
		}

		Optional<UnitOfMeasure> byName = repository.findByNameIgnoreCase(name.trim());
		if (byName.isPresent()) {
			throw new IllegalArgumentException("Unit name '" + name.trim() + "' already exists.");
		}
		Optional<UnitOfMeasure> byAbbr = repository.findByAbbreviationIgnoreCase(abbreviation.trim());
		if (byAbbr.isPresent()) {
			throw new IllegalArgumentException("Unit abbreviation '" + abbreviation.trim() + "' already exists.");
		}
	}

	// --- NEW: Centralized validation for name/abbreviation uniqueness on update
	// ---
	private void validateUniquenessOnUpdate(String name, String abbreviation, Long unitId) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Unit name cannot be blank.");
		}
		if (!StringUtils.hasText(abbreviation)) {
			throw new IllegalArgumentException("Unit abbreviation cannot be blank.");
		}

		Optional<UnitOfMeasure> byName = repository.findByNameIgnoreCase(name.trim());
		if (byName.isPresent() && !byName.get().getId().equals(unitId)) {
			throw new IllegalArgumentException("Unit name '" + name.trim() + "' already exists.");
		}

		Optional<UnitOfMeasure> byAbbr = repository.findByAbbreviationIgnoreCase(abbreviation.trim());
		if (byAbbr.isPresent() && !byAbbr.get().getId().equals(unitId)) {
			throw new IllegalArgumentException("Unit abbreviation '" + abbreviation.trim() + "' already exists.");
		}
	}
	// --- END NEW ---

	@Override
	@Transactional(readOnly = true)
	public List<UnitOfMeasure> findAll() {
		return repository.findAll(); // Consider sorting
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UnitOfMeasure> findById(Long id) {
		return repository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UnitOfMeasure> findByNameOrAbbreviation(String name, String abbreviation) {
		// Keep this method for checks, but validation is now separate
		if (StringUtils.hasText(name)) {
			Optional<UnitOfMeasure> byName = repository.findByNameIgnoreCase(name.trim());
			if (byName.isPresent())
				return byName;
		}
		if (StringUtils.hasText(abbreviation)) {
			return repository.findByAbbreviationIgnoreCase(abbreviation.trim());
		}
		return Optional.empty();
	}

	@Override
	public UnitOfMeasure save(UnitOfMeasure unit) {
		// Basic validation before saving directly
		if (unit == null || !StringUtils.hasText(unit.getName()) || !StringUtils.hasText(unit.getAbbreviation())) {
			throw new IllegalArgumentException("Cannot save unit with null or blank name/abbreviation.");
		}
		// Note: This basic save doesn't re-check for duplicates
		return repository.save(unit);
	}

	// --- NEW: Save method using DTO with validation ---
	@Override
	public UnitOfMeasure saveFromDto(UnitOfMeasureDto unitDto) {
		if (unitDto == null) {
			throw new IllegalArgumentException("Unit data cannot be null.");
		}
		// Validate uniqueness before proceeding
		validateUniqueness(unitDto.getName(), unitDto.getAbbreviation());

		UnitOfMeasure newUnit = new UnitOfMeasure(unitDto.getName().trim(), unitDto.getAbbreviation().trim());
		try {
			UnitOfMeasure savedUnit = repository.save(newUnit);
			log.info("Saved new unit: ID={}, Name='{}', Abbreviation='{}'", savedUnit.getId(), savedUnit.getName(),
					savedUnit.getAbbreviation());
			return savedUnit;
		} catch (Exception e) {
			log.error("Database error saving unit '{}': {}", unitDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not save unit due to a database error.", e);
		}
	}
	// --- End NEW Method ---

	// --- NEW: Update method using DTO with validation ---
	@Override
	public UnitOfMeasure updateFromDto(UnitOfMeasureDto unitDto) {
		if (unitDto == null || unitDto.getId() == null) {
			throw new IllegalArgumentException("Unit data or ID cannot be null for update.");
		}

		// 1. Find the existing unit
		UnitOfMeasure unitToUpdate = repository.findById(unitDto.getId())
				.orElseThrow(() -> new RuntimeException("Unit not found with id: " + unitDto.getId()));

		// 2. Validate the new name and abbreviation
		validateUniquenessOnUpdate(unitDto.getName(), unitDto.getAbbreviation(), unitDto.getId());

		// 3. Update the fields
		unitToUpdate.setName(unitDto.getName().trim());
		unitToUpdate.setAbbreviation(unitDto.getAbbreviation().trim());

		try {
			// 4. Save
			UnitOfMeasure savedUnit = repository.save(unitToUpdate);
			log.info("Updated unit: ID={}, Name='{}', Abbreviation='{}'", savedUnit.getId(), savedUnit.getName(),
					savedUnit.getAbbreviation());
			return savedUnit;
		} catch (Exception e) {
			log.error("Database error updating unit '{}': {}", unitDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not update unit due to a database error.", e);
		}
	}
	// --- End NEW Method ---

	// **** METHOD UPDATED ****
	@Override
	public void deleteById(Long id) {
		// 1. Find the unit first
		UnitOfMeasure unit = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Unit not found with id: " + id));

		// 2. Check if any items are using this unit
		// This is not efficient, but it matches the controller's old logic.
		// A repository method `countByUnitId` would be better.
		List<InventoryItem> itemsUsingUnit = inventoryItemService.findAll().stream()
				.filter(item -> item.getUnit() != null && item.getUnit().getId().equals(id))
				.collect(Collectors.toList());

		if (!itemsUsingUnit.isEmpty()) {
			throw new RuntimeException("Cannot delete unit '" + unit.getName() + "'. It is associated with "
					+ itemsUsingUnit.size() + " inventory item(s).");
		}

		// 3. If no items, proceed with deletion
		repository.deleteById(id);
		log.info("Deleted unit with ID: {}", id);
	}
	// **** END OF UPDATED METHOD ****

	// **** ERRONEOUS METHOD REMOVED ****

}