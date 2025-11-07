package com.toastedsiopao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; // Import LocalDateTime

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

	// --- THIS IS THE MAJOR CHANGE ---
	@ManyToOne(fetch = FetchType.EAGER) // Eagerly fetch the role, we always need it
	@JoinColumn(name = "role_id") // This user table will have a "role_id" foreign key
	private Role role; // e.g., "ROLE_CUSTOMER", "ROLE_ADMIN"
	// --- END CHANGE ---

	@Column(length = 50)
	private String firstName;

	@Column(length = 50)
	private String lastName;

	// **** NEW FIELD ****
	@Column(unique = true, length = 100) // Email should be unique
	private String email;
	// **** END NEW FIELD ****

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

	// --- NEW: Status and Activity Tracking (Made nullable for migration) ---
	@Column(nullable = true, length = 20) // FIX: Was nullable=false
	private String status; // "ACTIVE", "INACTIVE"

	@Column(nullable = true, updatable = false) // FIX: Was nullable=false
	private LocalDateTime createdAt;

	@Column(nullable = true) // This one was already correct
	private LocalDateTime lastActivity;
	// --- END NEW ---

	// Constructor for convenience (still useful for initial user creation)
	public User(String username, String password, Role role) {
		this.username = username;
		this.password = password;
		this.role = role;
	}

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (lastActivity == null) {
			lastActivity = LocalDateTime.now(); // Set initial activity to creation time
		}
		if (status == null) {
			status = "ACTIVE"; // Default status
		}
	}
}