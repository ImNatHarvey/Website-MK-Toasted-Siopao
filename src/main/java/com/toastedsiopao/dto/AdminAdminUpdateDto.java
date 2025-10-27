package com.toastedsiopao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdminAdminUpdateDto {

	private Long id; // Required for update

	@NotBlank(message = "First name cannot be blank")
	@Size(min = 2, max = 50, message = "First name length must be 2-50 characters")
	private String firstName;

	@NotBlank(message = "Last name cannot be blank")
	@Size(min = 2, max = 50, message = "Last name length must be 2-50 characters")
	private String lastName;

	@NotBlank(message = "Username cannot be blank")
	@Size(min = 3, max = 50, message = "Username length must be 3-50 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
	private String username;
}