package com.toastedsiopao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoleDto {

	private Long id;

	@NotBlank(message = "Role name cannot be blank")
	@Size(min = 2, max = 50, message = "Role name length must be 2-50 characters")
	@Pattern(regexp = "^[a-zA-Z_]+$", message = "Role name can only contain letters and underscores (e.g., 'SUPERVISOR')")
	private String name;

	private boolean manageCustomers;
	private boolean manageAdmins;
	private boolean manageOrders;
	private boolean manageProducts;
	private boolean manageInventory;
	private boolean manageTransactions;
	private boolean manageSite;
	private boolean manageActivityLog;
}