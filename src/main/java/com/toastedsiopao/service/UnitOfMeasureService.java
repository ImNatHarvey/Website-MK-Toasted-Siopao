package com.toastedsiopao.service;

import com.toastedsiopao.model.UnitOfMeasure;
import java.util.List;
import java.util.Optional;

public interface UnitOfMeasureService {
	List<UnitOfMeasure> findAll();

	Optional<UnitOfMeasure> findById(Long id);

	Optional<UnitOfMeasure> findByNameOrAbbreviation(String name, String abbreviation);

	UnitOfMeasure save(UnitOfMeasure unit);

	void deleteById(Long id);
}