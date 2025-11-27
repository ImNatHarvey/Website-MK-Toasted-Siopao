package com.toastedsiopao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty; 
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ProductDto {

	private Long id;

	@NotBlank(message = "• Product name cannot be blank")
	@Size(max = 100, message = "• Product name cannot exceed 100 characters")
	private String name;

	@Size(max = 500, message = "• Description cannot exceed 500 characters")
	private String description;

	@NotNull(message = "• Price cannot be null")
	@PositiveOrZero(message = "• Price must be zero or positive") 
	private BigDecimal price;

	@NotNull(message = "• Category must be selected")
	private Long categoryId;

	private String imageUrl;

	@Valid
	@NotEmpty(message = "• A product must have at least one ingredient")
	private List<RecipeIngredientDto> ingredients = new ArrayList<>();

	@NotNull(message = "• Low stock threshold cannot be null")
	@Min(value = 1, message = "• Low threshold must be at least 1")
	private Integer lowStockThreshold;
	
	@NotNull(message = "• Critical stock threshold cannot be null")
	@Min(value = 1, message = "• Critical threshold must be at least 1")
	private Integer criticalStockThreshold;
	
	private boolean removeImage = false;
	
	@NotBlank(message = "• Status must be selected")
	@Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "• Status must be either ACTIVE or INACTIVE")
	private String productStatus = "ACTIVE";
	
	// --- ADDED ---
	private LocalDate createdDate; // Defaults to today if null
	
	@Min(value = 0, message = "• Expiration days cannot be negative")
	private Integer expirationDays; // 0 or null means no expiration
	// --- END ADDED ---
}