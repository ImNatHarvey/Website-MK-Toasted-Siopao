package com.toastedsiopao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero; // Keep PositiveOrZero for Integer
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ProductDto {

	private Long id;

	@NotBlank(message = "Product name cannot be blank")
	@Size(max = 100, message = "Product name cannot exceed 100 characters")
	private String name;

	@Size(max = 500, message = "Description cannot exceed 500 characters")
	private String description;

	@NotNull(message = "Price cannot be null")
	@PositiveOrZero(message = "Price must be zero or positive") // Keep for BigDecimal
	private BigDecimal price;

	@NotNull(message = "Category must be selected")
	private Long categoryId;

	private String imageUrl;

	@Valid
	private List<RecipeIngredientDto> ingredients = new ArrayList<>();

	// --- NEW: Stock Management Fields ---
	// Current stock is usually adjusted separately, not set directly in
	// create/update DTO
	// We might add it later if needed for initial creation, but thresholds are
	// primary here.

	@NotNull(message = "Low stock threshold cannot be null")
	@PositiveOrZero(message = "Low threshold must be zero or positive")
	private Integer lowStockThreshold = 0;

	@NotNull(message = "Critical stock threshold cannot be null")
	@PositiveOrZero(message = "Critical threshold must be zero or positive")
	private Integer criticalStockThreshold = 0;
	// --- END NEW ---

}