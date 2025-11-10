package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
	private String name;

	@NotNull(message = "Category must be selected")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private InventoryCategory category;

	@NotNull(message = "Unit must be selected")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "unit_id", nullable = false)
	private UnitOfMeasure unit;

	@NotNull(message = "Current stock cannot be null")
	@PositiveOrZero(message = "Stock must be zero or positive")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal currentStock = BigDecimal.ZERO;

	@NotNull(message = "Low stock threshold cannot be null")
	@PositiveOrZero(message = "Threshold must be zero or positive")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal lowStockThreshold = BigDecimal.ZERO;

	@NotNull(message = "Critical stock threshold cannot be null")
	@PositiveOrZero(message = "Threshold must be zero or positive")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal criticalStockThreshold = BigDecimal.ZERO;

	@NotNull(message = "Cost per unit cannot be null")
	@PositiveOrZero(message = "Cost must be zero or positive")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal costPerUnit;

	private LocalDateTime lastUpdated;

	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		lastUpdated = LocalDateTime.now();
		if (criticalStockThreshold.compareTo(lowStockThreshold) > 0) {
			criticalStockThreshold = lowStockThreshold;
		}
	}

	public String getStockStatus() {
		if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
			return "NO_STOCK";
		} else if (currentStock.compareTo(criticalStockThreshold) <= 0) {
			return "CRITICAL";
		} else if (currentStock.compareTo(lowStockThreshold) <= 0) {
			return "LOW";
		} else {
			return "NORMAL";
		}
	}

	@Transient
	public BigDecimal getTotalCostValue() {
		if (costPerUnit == null || currentStock == null) {
			return BigDecimal.ZERO;
		}
		return costPerUnit.multiply(currentStock);
	}

	@Transient
	public int getLowStockPercentage() {
		if (lowStockThreshold.compareTo(BigDecimal.ZERO) > 0) {
			return criticalStockThreshold.multiply(BigDecimal.valueOf(100))
					.divide(lowStockThreshold, 0, BigDecimal.ROUND_HALF_UP).intValue();
		}
		return 20;
	}

	@Transient
	public int getCriticalStockPercentage() {
		return 5;
	}
}