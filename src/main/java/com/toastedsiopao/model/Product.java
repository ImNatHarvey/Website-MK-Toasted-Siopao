package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList; // Import ArrayList
import java.util.List; // Import List

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

	// --- NEW: Relationship to Ingredients ---
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<RecipeIngredient> ingredients = new ArrayList<>();
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