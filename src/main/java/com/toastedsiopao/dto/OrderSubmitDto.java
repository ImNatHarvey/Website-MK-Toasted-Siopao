package com.toastedsiopao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderSubmitDto {

	// Personal Details
	@NotBlank(message = "• First name is required.")
	@Pattern(regexp = "^(?! )[A-Za-z\\s]+(?<! )$", message = "• First name must contain only letters and single spaces, and cannot start or end with a space")
	private String firstName;

	@NotBlank(message = "• Last name is required.")
	@Pattern(regexp = "^(?! )[A-Za-z\\s]+(?<! )$", message = "• Last name must contain only letters and single spaces, and cannot start or end with a space")
	private String lastName;

	@NotBlank(message = "• Phone number is required.")
	@Pattern(regexp = "^(09|\\+639)\\d{9}$", message = "• Invalid Philippine phone number format.")
	private String phone;

	@NotBlank(message = "• Email is required.")
	@Email(message = "• Invalid email format.")
	@Size(max = 100, message = "• Email cannot exceed 100 characters")
	private String email;

	@Size(max = 50, message = "• House No. cannot exceed 50 characters")
	private String houseNo;

	@Size(max = 50, message = "• Lot No. cannot exceed 50 characters")
	private String lotNo;

	@Size(max = 50, message = "• Block No. cannot exceed 50 characters")
	private String blockNo;

	@NotBlank(message = "• Street / Subdivision is required.")
	@Size(max = 100, message = "• Street cannot exceed 100 characters")
	@Pattern(regexp = "^(?! )[A-Za-z0-9\\s,.-]+(?<! )$", message = "• Invalid street format.")
	private String street;

	@NotBlank(message = "• Barangay is required.")
	@Size(max = 100, message = "• Barangay cannot exceed 100 characters")
	@Pattern(regexp = "^(?! )[A-Za-z0-9\\s,.-]+(?<! )$", message = "• Invalid barangay format.")
	private String barangay;

	@NotBlank(message = "• Municipality is required.")
	@Size(max = 100, message = "• Municipality cannot exceed 100 characters")
	@Pattern(regexp = "^(?! )[A-Za-z\\s,.-]+(?<! )$", message = "• Invalid municipality format.")
	private String municipality;

	@NotBlank(message = "• Province is required.")
	@Size(max = 100, message = "• Province cannot exceed 100 characters")
	@Pattern(regexp = "^(?! )[A-Za-z\\s,.-]+(?<! )$", message = "• Invalid province format.")
	private String province;

	@NotBlank(message = "• Payment method is required.")
	private String paymentMethod; 

	@Size(max = 500, message = "• Notes cannot exceed 500 characters")
	private String notes;

	private String cartDataJson; 
	
	private String transactionId;

	// --- Custom Setters for trimming and normalizing internal whitespace ---
	public void setFirstName(String firstName) {
		this.firstName = (firstName == null) ? null : firstName.trim().replaceAll("\\s+", " ");
	}

	public void setLastName(String lastName) {
		this.lastName = (lastName == null) ? null : lastName.trim().replaceAll("\\s+", " ");
	}
	
	public void setHouseNo(String houseNo) {
		this.houseNo = (houseNo == null) ? null : houseNo.trim();
	}
	
	public void setLotNo(String lotNo) {
		this.lotNo = (lotNo == null) ? null : lotNo.trim();
	}
	
	public void setBlockNo(String blockNo) {
		this.blockNo = (blockNo == null) ? null : blockNo.trim();
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