package com.toastedsiopao.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {

	// --- Basic Info ---
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

	// **** NEW FIELD ****
	@NotBlank(message = "Email cannot be blank")
	@Email(message = "Invalid email format")
	@Size(max = 100, message = "Email cannot exceed 100 characters")
	private String email;
	// **** END NEW FIELD ****

	@NotBlank(message = "Phone number cannot be blank")
	@Pattern(regexp = "^(09|\\+639)\\d{9}$", message = "Invalid Philippine phone number format (e.g., 09xxxxxxxxx or +639xxxxxxxxx)")
	private String phone;

	// --- Location Details (Add @NotBlank if a field is truly required) ---
	@Size(max = 50, message = "House No. cannot exceed 50 characters")
	private String houseNo;

	@Size(max = 50, message = "Lot No. cannot exceed 50 characters")
	private String lotNo;

	@Size(max = 50, message = "Block No. cannot exceed 50 characters")
	private String blockNo;

	@NotBlank(message = "Street / Subdivision cannot be blank")
	@Size(max = 100, message = "Street cannot exceed 100 characters")
	private String street;

	@NotBlank(message = "Barangay cannot be blank")
	@Size(max = 100, message = "Barangay cannot exceed 100 characters")
	private String barangay;

	@NotBlank(message = "Municipality cannot be blank")
	@Size(max = 100, message = "Municipality cannot exceed 100 characters")
	private String municipality;

	@NotBlank(message = "Province cannot be blank")
	@Size(max = 100, message = "Province cannot exceed 100 characters")
	private String province;

	// --- Password ---
	@NotBlank(message = "Password cannot be blank")
	@Size(min = 8, message = "Password must be at least 8 characters")
	@Pattern(regexp = "^\\S+$", message = "Password cannot contain any spaces")
	private String password;

	@NotBlank(message = "Confirm password cannot be blank")
	private String confirmPassword;

	// --- Terms ---
	@AssertTrue(message = "You must agree to the terms of service")
	private boolean terms;
}