package com.toastedsiopao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetDto {

	@NotBlank(message = "Token cannot be blank")
	private String token;

	@NotBlank(message = "Password cannot be blank")
	@Size(min = 8, message = "Password must be at least 8 characters")
	@Pattern(regexp = "^\\S+$", message = "Password cannot contain any spaces")
	private String password;

	@NotBlank(message = "Confirm password cannot be blank")
	private String confirmPassword;
}