package com.toastedsiopao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UnitOfMeasureDto {

	private Long id; 

	@NotBlank(message = "• Unit name cannot be blank")
	@Size(max = 20, message = "• Unit name cannot exceed 20 characters")
	private String name;

	@NotBlank(message = "• Abbreviation cannot be blank")
	@Size(max = 10, message = "• Abbreviation cannot exceed 10 characters")
	private String abbreviation;
}