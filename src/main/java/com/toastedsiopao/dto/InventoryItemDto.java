package com.toastedsiopao.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class InventoryItemDto {

	private Long id; // For editing

	@NotBlank(message = "Item name cannot be blank")
	@Size(max = 100, message = "Item name cannot exceed 100 characters")
	private String name;

	@NotNull(message = "Category must be selected")
	private Long categoryId;

	@NotNull(message = "Unit must be selected")
	private Long unitId;

	@NotNull(message = "Current stock cannot be null")
	@PositiveOrZero(message = "Stock must be zero or positive")
	private BigDecimal currentStock = BigDecimal.ZERO;

	// --- UPDATED: Changed value from "0.0" to "1.0" ---
	@NotNull(message = "Low stock threshold cannot be null")
	@DecimalMin(value = "1.0", message = "Low threshold must be at least 1") // Use DecimalMin for BigDecimal
	private BigDecimal lowStockThreshold;
	// --- END UPDATE ---

	// --- UPDATED: Changed value from "0.0" to "1.0" ---
	@NotNull(message = "Critical stock threshold cannot be null")
	@DecimalMin(value = "1.0", message = "Critical threshold must be at least 1")
	private BigDecimal criticalStockThreshold;
	// --- END UPDATE ---

	// --- UPDATED ---
	@NotNull(message = "Cost per unit cannot be null")
	@DecimalMin(value = "0.0", message = "Cost must be zero or positive")
	private BigDecimal costPerUnit;
	// --- END UPDATE ---
}