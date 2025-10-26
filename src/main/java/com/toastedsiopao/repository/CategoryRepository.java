package com.toastedsiopao.repository;

import com.toastedsiopao.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Find category by name (useful for checking duplicates)
    Optional<Category> findByNameIgnoreCase(String name);
}
