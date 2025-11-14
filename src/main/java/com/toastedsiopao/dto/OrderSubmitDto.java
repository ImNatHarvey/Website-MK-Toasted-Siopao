package com.toastedsiopao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

// This DTO captures the form submission from the customer's order page
@Data
@NoArgsConstructor
public class OrderSubmitDto {

	// Personal Details
	@NotBlank(message = "First name is required.")
	private String firstName;

	@NotBlank(message = "Last name is required.")
	private String lastName;

	@NotBlank(message = "Phone number is required.")
	@Pattern(regexp = "^(09|\\+639)\\d{9}$", message = "Invalid Philippine phone number format.")
	private String phone;

	@NotBlank(message = "Email is required.")
	@Email(message = "Invalid email format.")
	private String email;

	// Location Details
	@Size(max = 50, message = "House No. cannot exceed 50 characters")
	private String houseNo;

	@Size(max = 50, message = "Lot No. cannot exceed 50 characters")
	private String lotNo;

	@Size(max = 50, message = "Block No. cannot exceed 50 characters")
	private String blockNo;

	@NotBlank(message = "Street / Subdivision is required.")
	@Size(max = 100, message = "Street cannot exceed 100 characters")
	private String street;

	@NotBlank(message = "Barangay is required.")
	@Size(max = 100, message = "Barangay cannot exceed 100 characters")
	private String barangay;

	@NotBlank(message = "Municipality is required.")
	@Size(max = 100, message = "Municipality cannot exceed 100 characters")
	private String municipality;

	@NotBlank(message = "Province is required.")
	@Size(max = 100, message = "Province cannot exceed 100 characters")
	private String province;

	// Order Details
	@NotBlank(message = "Payment method is required.")
	private String paymentMethod; // "gcash" or "cod"

	@Size(max = 500, message = "Notes cannot exceed 500 characters")
	private String notes;

	@NotBlank(message = "Cart data cannot be empty.")
	private String cartDataJson; // A JSON string of the cart from sessionStorage
	
	// --- ADDED ---
	@Size(max = 255, message = "Transaction ID cannot exceed 255 characters")
	private String transactionId;
	// --- END ADDED ---
}