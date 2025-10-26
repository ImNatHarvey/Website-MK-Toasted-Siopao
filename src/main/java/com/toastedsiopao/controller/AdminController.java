package com.toastedsiopao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin") // All URLs in this controller will start with /admin
public class AdminController {

	// --- REMOVED adminLogin() method ---

	@GetMapping("/dashboard")
	public String adminDashboard() {
		return "admin/dashboard";
	}

	@GetMapping("/orders")
	public String manageOrders() {
		return "admin/orders";
	}

	@GetMapping("/customers")
	public String manageCustomers() {
		return "admin/customers";
	}

	@GetMapping("/products")
	public String manageProducts() {
		return "admin/products";
	}

	@GetMapping("/inventory")
	public String manageInventory() {
		return "admin/inventory";
	}

	@GetMapping("/transactions")
	public String viewTransactions() {
		return "admin/transactions";
	}

	@GetMapping("/settings")
	public String siteSettings() {
		return "admin/settings";
	}

}
