package com.toastedsiopao.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class InventoryItemDto {

	private Long id; 

	@NotBlank(message = "• Item name cannot be blank")
	@Size(max = 100, message = "• Item name cannot exceed 100 characters")
	private String name;

	@NotNull(message = "• Category must be selected")
	private Long categoryId;

	@NotNull(message = "• Unit must be selected")
	private Long unitId;

	@NotNull(message = "• Current stock cannot be null")
	@PositiveOrZero(message = "• Stock must be zero or positive")
	private BigDecimal currentStock = BigDecimal.ZERO;

	@NotNull(message = "• Low stock threshold cannot be null")
	@DecimalMin(value = "1.0", message = "• Low threshold must be at least 1")
	private BigDecimal lowStockThreshold;
	
	@NotNull(message = "• Critical stock threshold cannot be null")
	@DecimalMin(value = "1.0", message = "• Critical threshold must be at least 1")
	private BigDecimal criticalStockThreshold;

	@NotNull(message = "• Cost per unit cannot be null")
	@DecimalMin(value = "0.0", message = "• Cost must be zero or positive")
	private BigDecimal costPerUnit;
	
	@NotBlank(message = "• Status must be selected")
	@Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "• Status must be either ACTIVE or INACTIVE")
	private String itemStatus;
	
	// --- NEW FIELDS ---
	private LocalDate receivedDate; // Defaults to today in entity if null
	private LocalDate expirationDate; // Null or future date
}