package com.toastedsiopao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Username cannot be blank")
	@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@NotBlank(message = "Password cannot be blank")
	@Column(nullable = false, length = 68) // BCrypt hash length
	private String password;

	@NotBlank(message = "Role cannot be blank")
	@Column(nullable = false, length = 20)
	private String role; // "ROLE_CUSTOMER", "ROLE_ADMIN"

	@Column(length = 50)
	private String firstName;

	@Column(length = 50)
	private String lastName;

	@Column(length = 20)
	private String phone;

	// --- NEW ADDRESS FIELDS ---
	@Column(length = 50, nullable = true) // Optional fields
	private String houseNo;

	@Column(length = 50, nullable = true)
	private String lotNo;

	@Column(length = 50, nullable = true)
	private String blockNo;

	@Column(length = 100, nullable = true) // Made nullable, adjust if street is required
	private String street;

	@Column(length = 100, nullable = true) // Made nullable, adjust if barangay is required
	private String barangay;

	@Column(length = 100, nullable = true) // Made nullable, adjust if municipality is required
	private String municipality;

	@Column(length = 100, nullable = true) // Made nullable, adjust if province is required
	private String province;
	// --- END NEW ADDRESS FIELDS ---

	// Constructor for convenience (still useful for initial user creation)
	public User(String username, String password, String role) {
		this.username = username;
		this.password = password;
		this.role = role;
	}
}
