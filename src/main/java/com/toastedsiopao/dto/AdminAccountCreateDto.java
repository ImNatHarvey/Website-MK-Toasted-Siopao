package com.toastedsiopao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdminAccountCreateDto {

	@NotBlank(message = "• First name cannot be blank")
	@Size(min = 2, max = 50, message = "• First name length must be 2-50 characters")
	@Pattern(regexp = "^(?! )[A-Za-z\\s]+(?<! )$", message = "• First name must contain only letters and single spaces, and cannot start or end with a space")
	private String firstName;

	@NotBlank(message = "• Last name cannot be blank")
	@Size(min = 2, max = 50, message = "• Last name length must be 2-50 characters")
	@Pattern(regexp = "^(?! )[A-Za-z\\s]+(?<! )$", message = "• Last name must contain only letters and single spaces, and cannot start or end with a space")
	private String lastName;

	@NotBlank(message = "• Username cannot be blank")
	@Size(min = 3, max = 50, message = "• Username length must be 3-50 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "• Username can only contain letters, numbers, and underscores")
	private String username;

	@NotBlank(message = "• Email cannot be blank")
	@Email(message = "• Invalid email format")
	@Size(max = 100, message = "• Email cannot exceed 100 characters")
	private String email;

	@NotBlank(message = "• Password cannot be blank")
	@Size(min = 8, message = "• Password must be at least 8 characters")
	@Pattern(regexp = "^\\S+$", message = "• Password cannot contain any spaces")
	private String password;

	@NotBlank(message = "• Confirm password cannot be blank")
	private String confirmPassword;

	@NotBlank(message = "• A role must be selected")
	private String roleName;
	
	// --- Custom Setters for trimming and normalizing internal whitespace ---
	public void setFirstName(String firstName) {
		this.firstName = (firstName == null) ? null : firstName.trim().replaceAll("\\s+", " ");
	}

	public void setLastName(String lastName) {
		this.lastName = (lastName == null) ? null : lastName.trim().replaceAll("\\s+", " ");
	}
}