package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode; // IMPORT ADDED
import lombok.NoArgsConstructor;
import lombok.ToString; // IMPORT ADDED

import java.util.List;

@Entity
@Table(name = "inventory_categories")
@Data
@NoArgsConstructor
public class InventoryCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Category name cannot be blank")
	@Size(max = 50, message = "Category name cannot exceed 50 characters")
	@Column(nullable = false, unique = true, length = 50)
	private String name;

	@OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
	@EqualsAndHashCode.Exclude // --- THIS IS THE FIX ---
	@ToString.Exclude // --- THIS IS THE FIX ---
	private List<InventoryItem> items;

	public InventoryCategory(String name) {
		this.name = name;
	}
}