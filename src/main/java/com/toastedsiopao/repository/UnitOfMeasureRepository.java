package com.toastedsiopao.repository;

import com.toastedsiopao.model.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
	// Find unit by name or abbreviation (useful for checking duplicates)
	Optional<UnitOfMeasure> findByNameIgnoreCase(String name);

	Optional<UnitOfMeasure> findByAbbreviationIgnoreCase(String abbreviation);
}