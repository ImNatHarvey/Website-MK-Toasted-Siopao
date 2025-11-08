package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@NotNull(message = "Quantity cannot be null")
	@Min(value = 1, message = "Quantity must be at least 1")
	@Column(nullable = false)
	private Integer quantity;

	@NotNull(message = "Price per unit cannot be null")
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal pricePerUnit;

	public OrderItem(Product product, Integer quantity, BigDecimal pricePerUnit) {
		this.product = product;
		this.quantity = quantity;
		this.pricePerUnit = pricePerUnit;
	}

	public BigDecimal getTotalPrice() {
		if (pricePerUnit == null || quantity == null) {
			return BigDecimal.ZERO;
		}
		return pricePerUnit.multiply(new BigDecimal(quantity));
	}
}