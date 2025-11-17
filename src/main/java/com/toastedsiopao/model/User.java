package com.toastedsiopao.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable; // REMOVED
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection; // REMOVED
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany; // --- IMPORT ADDED ---
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList; // --- IMPORT ADDED ---
import java.util.HashSet;
import java.util.List; // --- IMPORT ADDED ---
import java.util.Set;

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
	@Column(nullable = false, length = 68)
	private String password;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id")
	private Role role;

	// --- REMOVED: Individual permission overrides ---
	// @ElementCollection(fetch = FetchType.EAGER)
	// @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name =
	// "user_id"))
	// @Column(name = "permission", nullable = false)
	// private Set<String> permissions = new HashSet<>();
	// --- END REMOVED ---
	
	// --- START: NEW RELATIONSHIP ---
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<IssueReport> issueReports = new ArrayList<>();
	// --- END: NEW RELATIONSHIP ---

	@Column(length = 50)
	private String firstName;

	@Column(length = 50)
	private String lastName;

	@Column(unique = true, length = 100)
	private String email;

	@Column(length = 20)
	private String phone;

	@Column(length = 50, nullable = true)
	private String houseNo;

	@Column(length = 50, nullable = true)
	private String lotNo;

	@Column(length = 50, nullable = true)
	private String blockNo;

	@Column(length = 100, nullable = true)
	private String street;

	@Column(length = 100, nullable = true)
	private String barangay;

	@Column(length = 100, nullable = true)
	private String municipality;

	@Column(length = 100, nullable = true)
	private String province;

	@Column(nullable = true, length = 20)
	private String status;

	@Column(nullable = true, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = true)
	private LocalDateTime lastActivity;

	// --- ADDED: Fields for password reset ---
	@Column(nullable = true)
	private String resetPasswordToken;

	@Column(nullable = true)
	private LocalDateTime resetPasswordTokenExpiry;
	// --- END ADDED ---

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
			lastActivity = LocalDateTime.now();
		}
		if (status == null) {
			status = "ACTIVE";
		}
	}

	// --- UPDATED: Helper method for permissions ---
	// public void addPermission(String permission) {
	// this.permissions.add(permission);
	// }

	@Transient
	public Set<String> getCombinedPermissions() {
		Set<String> combined = new HashSet<>();
		if (role != null) {
			combined.addAll(role.getPermissions());
		}
		// combined.addAll(this.permissions); // Removed user overrides
		return combined;
	}
}