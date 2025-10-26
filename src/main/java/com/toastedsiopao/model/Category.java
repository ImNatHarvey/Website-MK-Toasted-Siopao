package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Category name cannot be blank")
	@Size(max = 50, message = "Category name cannot exceed 50 characters")
	@Column(nullable = false, unique = true, length = 50)
	private String name;

	// Relationship: One Category can have many Products
	// 'mappedBy = "category"' refers to the 'category' field in the Product entity
	// CascadeType.ALL means operations (like delete) on Category cascade to
	// associated Products
	// FetchType.LAZY means products are not loaded unless explicitly requested
	@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<Product> products = new ArrayList<>();

	// Convenience constructor
	public Category(String name) {
		this.name = name;
	}
}
