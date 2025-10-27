package com.toastedsiopao.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // Changed table name to plural 'orders'
@Data
@NoArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// --- Customer Reference ---
	@ManyToOne(fetch = FetchType.LAZY) // Lazy load customer details unless needed
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// --- Order Details ---
	@Column(nullable = false, updatable = false)
	private LocalDateTime orderDate;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal totalAmount;

	@Column(nullable = false, length = 50)
	private String status; // e.g., PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED

	// --- Shipping Address (Copied at time of order) ---
	// We copy the address in case the user changes their default address later
	@Column(length = 100)
	private String shippingFirstName;

	@Column(length = 100)
	private String shippingLastName;

	@Column(length = 20)
	private String shippingPhone;

	@Column(length = 255) // Combined address string
	private String shippingAddress;

	// --- Payment Details ---
	@Column(length = 50)
	private String paymentMethod; // e.g., COD, GCASH

	@Column(length = 100)
	private String paymentStatus; // e.g., PENDING, PAID, REFUNDED

	// --- Order Items ---
	// One Order has Many OrderItems
	// CascadeType.ALL: If order is deleted, items are deleted.
	// orphanRemoval=true: If an item is removed from the list, it's deleted from
	// DB.
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	// --- Timestamps ---
	private LocalDateTime lastUpdated;

	@PrePersist
	protected void onCreate() {
		orderDate = LocalDateTime.now();
		lastUpdated = LocalDateTime.now();
		if (status == null) {
			status = "PENDING"; // Default status
		}
		if (paymentStatus == null) {
			paymentStatus = "PENDING"; // Default payment status
		}
	}

	@PreUpdate
	protected void onUpdate() {
		lastUpdated = LocalDateTime.now();
	}

	// --- Convenience method to add items ---
	public void addItem(OrderItem item) {
		items.add(item);
		item.setOrder(this);
	}

	public void removeItem(OrderItem item) {
		items.remove(item);
		item.setOrder(null);
	}
}