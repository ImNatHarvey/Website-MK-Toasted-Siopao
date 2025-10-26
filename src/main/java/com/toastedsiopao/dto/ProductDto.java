package com.toastedsiopao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ProductDto {

	private Long id; // For editing existing products

	@NotBlank(message = "Product name cannot be blank")
	@Size(max = 100, message = "Product name cannot exceed 100 characters")
	private String name;

	@Size(max = 500, message = "Description cannot exceed 500 characters")
	private String description;

	@NotNull(message = "Price cannot be null")
	@PositiveOrZero(message = "Price must be zero or positive")
	private BigDecimal price;

	@NotNull(message = "Category must be selected")
	private Long categoryId; // Reference to the Category's ID

	// We'll handle image URL separately, perhaps after saving the product
	// or store it directly if just linking an external URL for now.
	private String imageUrl;

	// Add fields like quantity, featured, available if needed
}
