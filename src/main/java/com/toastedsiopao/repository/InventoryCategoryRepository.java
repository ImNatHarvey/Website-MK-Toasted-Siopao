package com.toastedsiopao.repository;

import com.toastedsiopao.model.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {
	
	Optional<InventoryCategory> findByNameIgnoreCase(String name);
}