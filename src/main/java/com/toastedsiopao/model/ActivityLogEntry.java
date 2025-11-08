package com.toastedsiopao.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_log")
@Data
@NoArgsConstructor
public class ActivityLogEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private LocalDateTime timestamp;

	@Column(nullable = false, length = 50)
	private String username; 

	@Column(nullable = false, length = 255)
	private String action;

	@Column(length = 1000) 
	private String details;

	@PrePersist
	protected void onCreate() {
		timestamp = LocalDateTime.now();
	}
	
	public ActivityLogEntry(String username, String action, String details) {
		this.username = username;
		this.action = action;
		this.details = details;
	}

	public ActivityLogEntry(String username, String action) {
		this(username, action, null);
	}
}
