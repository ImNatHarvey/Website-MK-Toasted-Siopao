package com.toastedsiopao.repository;

import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

	// Find all ingredients for a specific product
	List<RecipeIngredient> findByProduct(Product product);

	// Find if a specific ingredient is used in any product recipe
	List<RecipeIngredient> findByInventoryItem(InventoryItem inventoryItem);
}