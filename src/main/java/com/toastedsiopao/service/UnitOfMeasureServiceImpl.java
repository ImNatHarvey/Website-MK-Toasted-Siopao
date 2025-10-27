package com.toastedsiopao.service;

import com.toastedsiopao.model.UnitOfMeasure;
import com.toastedsiopao.repository.UnitOfMeasureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UnitOfMeasureServiceImpl implements UnitOfMeasureService {

	@Autowired
	private UnitOfMeasureRepository repository;

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
		Optional<UnitOfMeasure> byName = repository.findByNameIgnoreCase(name);
		if (byName.isPresent()) {
			return byName;
		}
		return repository.findByAbbreviationIgnoreCase(abbreviation);
	}

	@Override
	public UnitOfMeasure save(UnitOfMeasure unit) {
		return repository.save(unit);
	}

	@Override
	public void deleteById(Long id) {
		// Add check later: Prevent deletion if unit is in use by items
		repository.deleteById(id);
	}
}