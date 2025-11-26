package com.toastedsiopao.dto;

import jakarta.validation.constraints.Email; 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerUpdateDto {

	private Long id; 

	@NotBlank(message = "First name cannot be blank")
	@Size(min = 2, max = 50, message = "First name length must be 2-50 characters")
	@Pattern(regexp = "^(?! )[A-Za-z\\s]+(?<! )$", message = "First name must contain only letters and single spaces, and cannot start or end with a space")
	private String firstName;

	@NotBlank(message = "Last name cannot be blank")
	@Size(min = 2, max = 50, message = "Last name length must be 2-50 characters")
	@Pattern(regexp = "^(?! )[A-Za-z\\s]+(?<! )$", message = "Last name must contain only letters and single spaces, and cannot start or end with a space")
	private String lastName;

	@NotBlank(message = "Username cannot be blank")
	@Size(min = 3, max = 50, message = "Username length must be 3-50 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
	private String username;

	@NotBlank(message = "Email cannot be blank")
	@Email(message = "Invalid email format")
	@Size(max = 100, message = "Email cannot exceed 100 characters")
	private String email;

	@Pattern(regexp = "^(09|\\+639)\\d{9}$", message = "Invalid Philippine phone number format (e.g., 09xxxxxxxxx or +639xxxxxxxxx)")
	private String phone;

	@Size(max = 50, message = "House No. cannot exceed 50 characters")
	private String houseNo;

	@Size(max = 50, message = "Lot No. cannot exceed 50 characters")
	private String lotNo;

	@Size(max = 50, message = "Block No. cannot exceed 50 characters")
	private String blockNo;

	@Size(max = 100, message = "Street cannot exceed 100 characters")
	@Pattern(regexp = "^(?! )[A-Za-z0-9\\s,.-]*", message = "Invalid street format (cannot start with a space)")
	private String street;

	@Size(max = 100, message = "Barangay cannot exceed 100 characters")
	@Pattern(regexp = "^(?! )[A-Za-z0-9\\s,.-]*", message = "Invalid barangay format (cannot start with a space)")
	private String barangay;

	@Size(max = 100, message = "Municipality cannot exceed 100 characters")
	@Pattern(regexp = "^(?! )[A-Za-z\\s,.-]*", message = "Invalid municipality format (cannot start with a space)")
	private String municipality;

	@Size(max = 100, message = "Province cannot exceed 100 characters")
	@Pattern(regexp = "^(?! )[A-Za-z\\s,.-]*", message = "Invalid province format (cannot start with a space)")
	private String province;

	@NotBlank(message = "Status must be selected")
	@Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be either ACTIVE or INACTIVE")
	private String status;
	
	// --- Custom Setters for trimming and normalizing internal whitespace ---
	public void setFirstName(String firstName) {
		this.firstName = (firstName == null) ? null : firstName.trim().replaceAll("\\s+", " ");
	}

	public void setLastName(String lastName) {
		this.lastName = (lastName == null) ? null : lastName.trim().replaceAll("\\s+", " ");
	}
	
	public void setStreet(String street) {
		this.street = (street == null) ? null : street.trim().replaceAll("\\s+", " ");
	}
	
	public void setBarangay(String barangay) {
		this.barangay = (barangay == null) ? null : barangay.trim().replaceAll("\\s+", " ");
	}
	
	public void setMunicipality(String municipality) {
		this.municipality = (municipality == null) ? null : municipality.trim().replaceAll("\\s+", " ");
	}
	
	public void setProvince(String province) {
		this.province = (province == null) ? null : province.trim().replaceAll("\\s+", " ");
	}
}