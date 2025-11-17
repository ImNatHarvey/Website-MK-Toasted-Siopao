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
@Table(name = "units_of_measure")
@Data
@NoArgsConstructor
public class UnitOfMeasure {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Unit name cannot be blank")
	@Size(max = 20, message = "Unit name cannot exceed 20 characters")
	@Column(nullable = false, unique = true, length = 20)
	private String name; 

	@NotBlank(message = "Abbreviation cannot be blank")
	@Size(max = 10, message = "Abbreviation cannot exceed 10 characters")
	@Column(nullable = false, unique = true, length = 10)
	private String abbreviation; 

	@OneToMany(mappedBy = "unit", fetch = FetchType.LAZY)
	@EqualsAndHashCode.Exclude // --- THIS IS THE FIX ---
	@ToString.Exclude // --- THIS IS THE FIX ---
	private List<InventoryItem> items;

	public UnitOfMeasure(String name, String abbreviation) {
		this.name = name;
		this.abbreviation = abbreviation;
	}
}