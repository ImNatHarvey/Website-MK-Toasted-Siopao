package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe_ingredients")
@Data
@NoArgsConstructor
public class RecipeIngredient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@NotNull
	@ManyToOne(fetch = FetchType.EAGER) 
	@JoinColumn(name = "inventory_item_id", nullable = false)
	private InventoryItem inventoryItem;

	@NotNull(message = "Quantity needed cannot be null")
	@Positive(message = "Quantity needed must be positive")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal quantityNeeded;
	
	public RecipeIngredient(Product product, InventoryItem inventoryItem, BigDecimal quantityNeeded) {
		this.product = product;
		this.inventoryItem = inventoryItem;
		this.quantityNeeded = quantityNeeded;
	}
}