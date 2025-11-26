package com.toastedsiopao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdminPasswordUpdateDto {

	@NotBlank(message = "• Current password cannot be blank")
	private String currentPassword;

	@NotBlank(message = "• New password cannot be blank")
	@Size(min = 8, message = "• Password must be at least 8 characters")
	@Pattern(regexp = "^\\S+$", message = "• Password cannot contain any spaces")
	private String newPassword;

	@NotBlank(message = "• Confirm password cannot be blank")
	private String confirmPassword;
}