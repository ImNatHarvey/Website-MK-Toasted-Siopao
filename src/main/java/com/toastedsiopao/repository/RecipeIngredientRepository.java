package com.toastedsiopao.repository;

import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; 
import org.springframework.data.repository.query.Param; 
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

	List<RecipeIngredient> findByProduct(Product product);

	List<RecipeIngredient> findByInventoryItem(InventoryItem inventoryItem);
	
	@Query("SELECT COUNT(ri) FROM RecipeIngredient ri WHERE ri.inventoryItem = :item")
	long countByInventoryItem(@Param("item") InventoryItem item);
}