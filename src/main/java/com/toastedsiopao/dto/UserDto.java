package com.toastedsiopao.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {

	@NotBlank(message = "First name cannot be blank")
	@Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
	private String firstName;

	@NotBlank(message = "Last name cannot be blank")
	@Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
	private String lastName;

	@NotBlank(message = "Username cannot be blank")
	@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
	private String username;

	@NotBlank(message = "Phone number cannot be blank")
	@Pattern(regexp = "^(09|\\+639)\\d{9}$", message = "Invalid Philippine phone number format (e.g., 09xxxxxxxxx or +639xxxxxxxxx)")
	private String phone;

	@NotBlank(message = "Password cannot be blank")
	// Add regex for password complexity if needed, e.g., using @Pattern
	@Size(min = 8, message = "Password must be at least 8 characters long")
	private String password;

	@NotBlank(message = "Confirm password cannot be blank")
	private String confirmPassword;

	@AssertTrue(message = "You must agree to the terms of service")
	private boolean terms;

	// We can add address fields later matching signup.html
	// private String houseNo;
	// private String lotNo;
	// ... etc ...
}
