package com.toastedsiopao.service;

import com.toastedsiopao.dto.UnitOfMeasureDto;
import com.toastedsiopao.model.UnitOfMeasure;
import com.toastedsiopao.repository.UnitOfMeasureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UnitOfMeasureServiceImpl implements UnitOfMeasureService {

	private static final Logger log = LoggerFactory.getLogger(UnitOfMeasureServiceImpl.class);

	@Autowired
	private UnitOfMeasureRepository repository;

	@Autowired
	private InventoryItemService inventoryItemService;

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

	@Override
	@Transactional(readOnly = true)
	public List<UnitOfMeasure> findAll() {
		return repository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UnitOfMeasure> findById(Long id) {
		return repository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UnitOfMeasure> findByNameOrAbbreviation(String name, String abbreviation) {
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
		if (unit == null || !StringUtils.hasText(unit.getName()) || !StringUtils.hasText(unit.getAbbreviation())) {
			throw new IllegalArgumentException("Cannot save unit with null or blank name/abbreviation.");
		}
		return repository.save(unit);
	}

	@Override
	public UnitOfMeasure saveFromDto(UnitOfMeasureDto unitDto) {
		if (unitDto == null) {
			throw new IllegalArgumentException("Unit data cannot be null.");
		}
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

	@Override
	public UnitOfMeasure updateFromDto(UnitOfMeasureDto unitDto) {
		if (unitDto == null || unitDto.getId() == null) {
			throw new IllegalArgumentException("Unit data or ID cannot be null for update.");
		}

		UnitOfMeasure unitToUpdate = repository.findById(unitDto.getId())
				.orElseThrow(() -> new RuntimeException("Unit not found with id: " + unitDto.getId()));

		validateUniquenessOnUpdate(unitDto.getName(), unitDto.getAbbreviation(), unitDto.getId());

		unitToUpdate.setName(unitDto.getName().trim());
		unitToUpdate.setAbbreviation(unitDto.getAbbreviation().trim());

		try {
			UnitOfMeasure savedUnit = repository.save(unitToUpdate);
			log.info("Updated unit: ID={}, Name='{}', Abbreviation='{}'", savedUnit.getId(), savedUnit.getName(),
					savedUnit.getAbbreviation());
			return savedUnit;
		} catch (Exception e) {
			log.error("Database error updating unit '{}': {}", unitDto.getName(), e.getMessage(), e);
			throw new RuntimeException("Could not update unit due to a database error.", e);
		}
	}

	@Override
	public void deleteById(Long id) {
		if (!repository.existsById(id)) {
			throw new RuntimeException("Unit not found with id: " + id);
		}

		repository.deleteById(id);

		log.info("Deleted unit with ID: {}", id);
	}
}