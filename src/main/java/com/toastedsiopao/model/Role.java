package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode; // IMPORT ADDED
import lombok.NoArgsConstructor;
import lombok.ToString; // IMPORT ADDED

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
	private String name;
	
	@ElementCollection(fetch = FetchType.EAGER) // Eager fetch roles, as we need them for security
	@CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "permission", nullable = false)
	@EqualsAndHashCode.Exclude // --- THIS IS THE FIX ---
	@ToString.Exclude // --- THIS IS THE FIX ---
	private Set<String> permissions = new HashSet<>();

	public Role(String name) {
		this.name = name;
	}

	public void addPermission(String permission) {
		this.permissions.add(permission);
	}
}