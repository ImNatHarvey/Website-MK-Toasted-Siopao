package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // Use BigDecimal for currency

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

	@Column(length = 500) // Allow longer descriptions
	private String description;

	@NotNull(message = "Price cannot be null")
	@PositiveOrZero(message = "Price must be zero or positive")
	@Column(nullable = false, precision = 10, scale = 2) // precision=total digits, scale=digits after decimal
	private BigDecimal price;

	// Optional: Track stock quantity if needed for inventory linking
	// @PositiveOrZero(message = "Quantity must be zero or positive")
	// private int quantity;

	@Column(length = 255) // Store the path/URL to the image
	private String imageUrl;

	// Relationship: Many Products belong to one Category
	// FetchType.EAGER means the category is always loaded with the product
	// JoinColumn specifies the foreign key column in the 'products' table
	@NotNull(message = "Product must belong to a category")
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	// Optional: Add flags for featured products, availability etc.
	// private boolean featured = false;
	// private boolean available = true;

}
