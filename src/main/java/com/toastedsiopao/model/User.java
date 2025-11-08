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

import java.time.LocalDateTime; 

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

	@ManyToOne(fetch = FetchType.EAGER) 
	@JoinColumn(name = "role_id")
	private Role role;

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
}