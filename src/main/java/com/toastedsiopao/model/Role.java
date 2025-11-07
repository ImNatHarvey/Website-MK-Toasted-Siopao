package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Role name cannot be blank")
	@Size(max = 50, message = "Role name cannot exceed 50 characters")
	@Column(nullable = false, unique = true, length = 50)
	private String name; // e.g., "Owner", "Manager", "Staff"

	/**
	 * A set of permissions associated with this role. We store the Permission
	 * enum's name (e.g., "VIEW_DASHBOARD") as a string.
	 */
	@ElementCollection(fetch = FetchType.EAGER) // Eager fetch roles, as we need them for security
	@CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "permission", nullable = false)
	// @Enumerated(EnumType.STRING) // REMOVED: No longer an enum
	private Set<String> permissions = new HashSet<>(); // UPDATED to Set<String>

	public Role(String name) {
		this.name = name;
	}

	// Convenience method
	public void addPermission(String permission) { // UPDATED to String
		this.permissions.add(permission);
	}
}