package com.toastedsiopao.controller;

import com.toastedsiopao.model.ActivityLogEntry; // Import
import com.toastedsiopao.service.ActivityLogService; // Import
import org.springframework.beans.factory.annotation.Autowired; // Import
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Import
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List; // Import

@Controller
@RequestMapping("/admin")
public class AdminController {

	// --- Inject ActivityLogService ---
	@Autowired
	private ActivityLogService activityLogService;
	// --- End Injection ---

	@GetMapping("/dashboard")
	public String adminDashboard() {
		return "admin/dashboard";
	}

	// --- NEW: Mapping for Activity Log Page ---
	@GetMapping("/activity-log")
	public String showActivityLog(Model model) {
		// Fetch all logs from the service (newest first)
		List<ActivityLogEntry> logs = activityLogService.getAllLogs();
		// Add the logs to the model so Thymeleaf can display them
		model.addAttribute("activityLogs", logs);
		return "admin/activity-log"; // Name of the new HTML file
	}
	// --- End New Mapping ---

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
