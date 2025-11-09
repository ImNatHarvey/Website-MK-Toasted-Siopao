package com.toastedsiopao.model;

public enum Permission {

	VIEW_DASHBOARD("View Dashboard"),

	VIEW_CUSTOMERS("View Customer Management"), ADD_CUSTOMERS("Add Customers"), EDIT_CUSTOMERS("Edit Customers"),
	DELETE_CUSTOMERS("Delete Customers"),

	VIEW_ADMINS("View Admin Management"), ADD_ADMINS("Add Admins"), EDIT_ADMINS("Edit Admins"), // ADDED
	DELETE_ADMINS("Delete Admins"),

	VIEW_ORDERS("View Order Management"), EDIT_ORDERS("Update Order Status"),

	VIEW_PRODUCTS("View Product Management"), ADD_PRODUCTS("Add Products"), EDIT_PRODUCTS("Edit Products"),
	DELETE_PRODUCTS("Delete Products"), ADJUST_PRODUCT_STOCK("Adjust Product Stock (Production/Wastage)"),

	VIEW_INVENTORY("View Inventory Management"), ADD_INVENTORY_ITEMS("Add Inventory Items"),
	EDIT_INVENTORY_ITEMS("Edit Inventory Items"), DELETE_INVENTORY_ITEMS("Delete Inventory Items"),
	ADJUST_INVENTORY_STOCK("Adjust Inventory Stock (Manual/Wastage)"),
	MANAGE_INVENTORY_CATEGORIES("Manage Inventory Categories"), MANAGE_UNITS("Manage Units of Measure"),

	VIEW_TRANSACTIONS("View Transaction History"),

	EDIT_SITE_SETTINGS("Edit Site Management"),

	VIEW_ACTIVITY_LOG("View Activity Log");

	private final String description;

	Permission(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}