package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // For potential cost tracking
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@Data
@NoArgsConstructor
public class InventoryItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Item name cannot be blank")
	@Size(max = 100, message = "Item name cannot exceed 100 characters")
	@Column(nullable = false, length = 100)
	private String name; // e.g., "All-Purpose Flour", "Ground Pork"

	@NotNull(message = "Category must be selected")
	@ManyToOne(fetch = FetchType.EAGER) // Usually want to see category name
	@JoinColumn(name = "category_id", nullable = false)
	private InventoryCategory category;

	@NotNull(message = "Unit must be selected")
	@ManyToOne(fetch = FetchType.EAGER) // Usually want to see unit name/abbreviation
	@JoinColumn(name = "unit_id", nullable = false)
	private UnitOfMeasure unit;

	@NotNull(message = "Current stock cannot be null")
	@PositiveOrZero(message = "Stock must be zero or positive")
	@Column(nullable = false, precision = 10, scale = 2) // Allow decimals for units like kg
	private BigDecimal currentStock = BigDecimal.ZERO;

	// --- Thresholds ---
	// Represents the quantity below which stock is considered "Low"
	@NotNull(message = "Low stock threshold cannot be null")
	@PositiveOrZero(message = "Threshold must be zero or positive")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal lowStockThreshold = BigDecimal.ZERO;

	// Represents the quantity below which stock is considered "Critical"
	@NotNull(message = "Critical stock threshold cannot be null")
	@PositiveOrZero(message = "Threshold must be zero or positive")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal criticalStockThreshold = BigDecimal.ZERO;

	// --- Optional Cost ---
	@PositiveOrZero(message = "Cost must be zero or positive")
	@Column(precision = 10, scale = 2)
	private BigDecimal costPerUnit; // Cost for one unit (e.g., cost per kg)

	private LocalDateTime lastUpdated;

	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		lastUpdated = LocalDateTime.now();
		// Ensure critical is not higher than low
		if (criticalStockThreshold.compareTo(lowStockThreshold) > 0) {
			criticalStockThreshold = lowStockThreshold;
		}
	}

	// --- Status Calculation Logic ---
	// Not stored in DB, calculated on the fly
	@Transient // Tells JPA not to map this field to a database column
	public String getStockStatus() {
		if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
			return "NO_STOCK"; // Black
		} else if (currentStock.compareTo(criticalStockThreshold) <= 0) {
			return "CRITICAL"; // Red
		} else if (currentStock.compareTo(lowStockThreshold) <= 0) {
			return "LOW"; // Yellow
		} else {
			return "NORMAL"; // Green
		}
	}

	// --- Threshold Percentages (Calculated for default view) ---
	// These are approximations if a 'max capacity' isn't defined
	// For simplicity, let's assume 'low' maps to 20% and 'critical' to 5%
	// of some reference point (e.g., the low threshold itself * 5)
	@Transient
	public int getLowStockPercentage() {
		// This is a simplification; a better approach might involve a max stock level.
		// If low threshold is 10, critical is 2, this returns 20.
		if (lowStockThreshold.compareTo(BigDecimal.ZERO) > 0) {
			return criticalStockThreshold.multiply(BigDecimal.valueOf(100))
					.divide(lowStockThreshold, 0, BigDecimal.ROUND_HALF_UP).intValue();
		}
		return 20; // Default if low threshold is 0
	}

	@Transient
	public int getCriticalStockPercentage() {
		return 5; // Default critical percentage representation
	}
}