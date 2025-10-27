package com.toastedsiopao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InventoryCategoryDto {

	private Long id; // For editing

	@NotBlank(message = "Category name cannot be blank")
	@Size(max = 50, message = "Category name cannot exceed 50 characters")
	private String name;
}