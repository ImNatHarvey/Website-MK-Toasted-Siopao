package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
	@Column(nullable = false, length = 100, unique = true)
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
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private Category category;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private List<RecipeIngredient> ingredients = new ArrayList<>();

	@NotNull(message = "Current stock cannot be null")
	@PositiveOrZero(message = "Stock must be zero or positive")
	@Column(nullable = false)
	private Integer currentStock = 0;

	@NotNull(message = "Low stock threshold cannot be null")
	@PositiveOrZero(message = "Threshold must be zero or positive")
	@Column(nullable = false)
	private Integer lowStockThreshold = 0;

	@NotNull(message = "Critical stock threshold cannot be null")
	@PositiveOrZero(message = "Threshold must be zero or positive")
	@Column(nullable = false)
	private Integer criticalStockThreshold = 0;

	private LocalDateTime stockLastUpdated;

	@Column(nullable = false)
	private boolean recipeLocked = false;
	
	@Column(nullable = false, length = 20)
	private String productStatus = "ACTIVE"; 
	
	// --- ADDED: Date Tracking Fields ---
	@Column(nullable = true)
	private LocalDate createdDate; // Date produced/stocked

	@Column(nullable = true)
	private LocalDate expirationDate; // Date it expires
	// --- END ADDED ---

	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		stockLastUpdated = LocalDateTime.now();
		if (criticalStockThreshold > lowStockThreshold) {
			criticalStockThreshold = lowStockThreshold;
		}
		// Ensure createdDate is set if null
		if (createdDate == null) {
			createdDate = LocalDate.now();
		}
	}

	@Transient
	public String getStockStatus() {
		if (currentStock <= 0) {
			return "NO_STOCK";
		} else if (currentStock <= criticalStockThreshold) {
			return "CRITICAL";
		} else if (currentStock <= lowStockThreshold) {
			return "LOW";
		} else {
			return "NORMAL";
		}
	}

	@Transient
	public String getPublicStockStatusText() {
		if (currentStock <= 0) {
			return "No Stock";
		} else {
			return "Available";
		}
	}

	@Transient
	public String getPublicStockStatusClass() {
		if (currentStock <= 0) {
			return "no_stock"; 
		} else {
			return "normal"; 
		}
	}
	
	// --- ADDED: Helper to calculate days for display ---
	@Transient
	public Integer getExpirationDays() {
		if (createdDate != null && expirationDate != null) {
			return (int) ChronoUnit.DAYS.between(createdDate, expirationDate);
		}
		return 0;
	}
	// --- END ADDED ---

	public void addIngredient(RecipeIngredient ingredient) {
		ingredients.add(ingredient);
		ingredient.setProduct(this);
	}

	public void removeIngredient(RecipeIngredient ingredient) {
		ingredients.remove(ingredient);
		ingredient.setProduct(null);
	}
}