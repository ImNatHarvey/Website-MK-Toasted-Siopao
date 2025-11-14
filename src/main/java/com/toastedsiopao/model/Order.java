package com.toastedsiopao.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") 
@Data
@NoArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY) 
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, updatable = false)
	private LocalDateTime orderDate;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal totalAmount;

	@Column(nullable = false, length = 50)
	private String status; 

	@Column(length = 100)
	private String shippingFirstName;

	@Column(length = 100)
	private String shippingLastName;

	@Column(length = 20)
	private String shippingPhone;

	@Column(length = 255)
	private String shippingAddress;

	@Column(length = 50)
	private String paymentMethod; 

	@Column(length = 100)
	private String paymentStatus; 
	
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	private LocalDateTime lastUpdated;
	
	@Column(length = 255)
	private String paymentReceiptImageUrl;

	@PrePersist
	protected void onCreate() {
		orderDate = LocalDateTime.now();
		lastUpdated = LocalDateTime.now();
		if (status == null) {
			status = "PENDING"; 
		}
		if (paymentStatus == null) {
			paymentStatus = "PENDING";
		}
	}

	@PreUpdate
	protected void onUpdate() {
		lastUpdated = LocalDateTime.now();
	}

	public void addItem(OrderItem item) {
		items.add(item);
		item.setOrder(this);
	}

	public void removeItem(OrderItem item) {
		items.remove(item);
		item.setOrder(null);
	}
}