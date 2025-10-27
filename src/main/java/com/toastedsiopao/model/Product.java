package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin; // Use DecimalMin for BigDecimal validation
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Import LocalDateTime
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Product name cannot be blank")
	@Size(max = 100, message = "Product name cannot exceed 100 characters")
	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@NotNull(message = "Price cannot be null")
	@PositiveOrZero(message = "Price must be zero or positive")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(length = 255)
	private String imageUrl;

	@NotNull(message = "Product must belong to a category")
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<RecipeIngredient> ingredients = new ArrayList<>();

	// --- NEW: Stock Management Fields ---
	@NotNull(message = "Current stock cannot be null")
	@PositiveOrZero(message = "Stock must be zero or positive")
	@Column(nullable = false)
	private Integer currentStock = 0; // Use Integer for countable products

	@NotNull(message = "Low stock threshold cannot be null")
	@PositiveOrZero(message = "Threshold must be zero or positive")
	@Column(nullable = false)
	private Integer lowStockThreshold = 0; // e.g., 10

	@NotNull(message = "Critical stock threshold cannot be null")
	@PositiveOrZero(message = "Threshold must be zero or positive")
	@Column(nullable = false)
	private Integer criticalStockThreshold = 0; // e.g., 5

	private LocalDateTime stockLastUpdated;
	// --- END NEW ---

	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		stockLastUpdated = LocalDateTime.now();
		// Ensure critical <= low
		if (criticalStockThreshold > lowStockThreshold) {
			criticalStockThreshold = lowStockThreshold;
		}
	}

	// --- NEW: Status Calculation Logic ---
	// Thresholds define the *point* at which the status changes.
	// Stock <= Critical Threshold -> CRITICAL (or NO_STOCK if 0)
	// Stock <= Low Threshold -> LOW
	// Stock > Low Threshold -> NORMAL
	@Transient
	public String getStockStatus() {
		if (currentStock <= 0) {
			return "NO_STOCK"; // Black
		} else if (currentStock <= criticalStockThreshold) {
			return "CRITICAL"; // Red
		} else if (currentStock <= lowStockThreshold) {
			return "LOW"; // Yellow
		} else {
			return "NORMAL"; // Green
		}
	}
	// --- END NEW ---

	// --- Convenience Methods for Ingredients ---
	public void addIngredient(RecipeIngredient ingredient) {
		ingredients.add(ingredient);
		ingredient.setProduct(this);
	}

	public void removeIngredient(RecipeIngredient ingredient) {
		ingredients.remove(ingredient);
		ingredient.setProduct(null);
	}
}