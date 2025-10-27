package com.toastedsiopao.service;

import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.repository.InventoryCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InventoryCategoryServiceImpl implements InventoryCategoryService {

	@Autowired
	private InventoryCategoryRepository repository;

	@Override
	@Transactional(readOnly = true)
	public List<InventoryCategory> findAll() {
		return repository.findAll(); // Consider adding sorting later
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<InventoryCategory> findById(Long id) {
		return repository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<InventoryCategory> findByName(String name) {
		return repository.findByNameIgnoreCase(name);
	}

	@Override
	public InventoryCategory save(InventoryCategory category) {
		return repository.save(category);
	}

	@Override
	public void deleteById(Long id) {
		// Add check later: Prevent deletion if category is in use by items
		repository.deleteById(id);
	}
}