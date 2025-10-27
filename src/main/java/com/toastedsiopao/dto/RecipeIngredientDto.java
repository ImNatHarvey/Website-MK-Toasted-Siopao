package com.toastedsiopao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class RecipeIngredientDto {

	private Long inventoryItemId; // ID of the InventoryItem used

	@NotNull(message = "Quantity needed cannot be null")
	@Positive(message = "Quantity needed must be positive")
	private BigDecimal quantityNeeded;

	// Optional: Add unit abbreviation if needed for display, though it comes from
	// InventoryItem
	// private String unitAbbreviation;
}