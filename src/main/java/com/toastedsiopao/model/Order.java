package com.toastedsiopao.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // IMPORT ADDED
import lombok.NoArgsConstructor;
import lombok.ToString; // IMPORT ADDED

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet; 
import java.util.List;
import java.util.Set; 

@Entity
@Table(name = "orders") 
@Data
@NoArgsConstructor
public class Order {

	// --- ADDED: Status Constants ---
	public static final String STATUS_PENDING = "PENDING";
	public static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";
	public static final String STATUS_PROCESSING = "PROCESSING";
	// --- NEW STATUS ---
	public static final String STATUS_OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
	// --- END NEW STATUS ---
	public static final String STATUS_DELIVERED = "DELIVERED";
	public static final String STATUS_CANCELLED = "CANCELLED";
	public static final String STATUS_REJECTED = "REJECTED"; 

	public static final String PAYMENT_PENDING = "PENDING";
	public static final String PAYMENT_FOR_VERIFICATION = "FOR_VERIFICATION";
	public static final String PAYMENT_PAID = "PAID";
	public static final String PAYMENT_REJECTED = "REJECTED"; 
	public static final String PAYMENT_CANCELLED = "CANCELLED"; 
	// --- END ADDED ---

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY) 
	@JoinColumn(name = "user_id", nullable = false)
	@EqualsAndHashCode.Exclude // --- THIS IS THE FIX ---
	@ToString.Exclude // --- THIS IS THE FIX ---
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
	
	@Column(length = 100)
	private String shippingEmail;
	
	@Column(length = 255)
	private String shippingAddress;

	@Column(length = 50)
	private String paymentMethod; 

	@Column(length = 100)
	private String paymentStatus; 
	
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@EqualsAndHashCode.Exclude // --- THIS IS THE FIX ---
	@ToString.Exclude // --- THIS IS THE FIX ---
	private List<OrderItem> items = new ArrayList<>();
	
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@EqualsAndHashCode.Exclude // --- THIS IS THE FIX ---
	@ToString.Exclude // --- THIS IS THE FIX ---
	private Set<IssueReport> issueReports = new HashSet<>();
	
	private LocalDateTime lastUpdated;
	
	@Column(length = 255)
	private String paymentReceiptImageUrl;
	
	@Column(length = 500)
	private String notes;
	
	@Column(length = 255)
	private String transactionId;
	
	@PrePersist
	protected void onCreate() {
		orderDate = LocalDateTime.now();
		lastUpdated = LocalDateTime.now();
		if (status == null) {
			status = STATUS_PENDING; 
		}
		if (paymentStatus == null) {
			paymentStatus = PAYMENT_PENDING; 
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