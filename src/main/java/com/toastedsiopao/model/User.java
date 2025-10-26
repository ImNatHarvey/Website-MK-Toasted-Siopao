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
@Table(name = "users") // Specify the table name
@Data // Lombok annotation for getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok annotation for no-args constructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Username cannot be blank")
	@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@NotBlank(message = "Password cannot be blank")
	@Column(nullable = false, length = 68) // Length accommodates BCrypt hashes
	private String password;

	@NotBlank(message = "Role cannot be blank")
	@Column(nullable = false, length = 20)
	private String role; // Examples: "ROLE_CUSTOMER", "ROLE_ADMIN"

	// Optional fields based on your signup form
	@Column(length = 50)
	private String firstName;

	@Column(length = 50)
	private String lastName;

	@Column(length = 20)
	private String phone;

	// We can add address fields later if needed

	// Constructor for convenience (optional, Lombok handles NoArgsConstructor)
	public User(String username, String password, String role) {
		this.username = username;
		this.password = password;
		this.role = role;
	}

}
