package com.toastedsiopao.controller;

import com.toastedsiopao.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/admin") // Base path for general admin pages
public class AdminDashboardController {

	private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

	// --- OrderService import and @Autowired field removed ---
	@Autowired
	private ActivityLogService activityLogService;

	@GetMapping("/dashboard")
	public String adminDashboard(Model model) {
		// Add logic here later to fetch dashboard stats if needed
		// For now, just return the view name
		return "admin/dashboard"; // Renders dashboard.html
	}

	// --- manageOrders method has been MOVED to AdminOrderController ---

	@GetMapping("/transactions")
	public String viewTransactions(Model model) {
		// Add logic later to fetch transaction data
		log.info("Accessing transaction history page"); // Add logging
		return "admin/transactions"; // Renders transactions.html
	}

	@GetMapping("/settings")
	public String siteSettings(Model model) {
		// Add logic later to fetch site settings
		log.info("Accessing site settings page"); // Add logging
		return "admin/settings"; // Renders settings.html
	}

	@GetMapping("/activity-log")
	public String showActivityLog(Model model) {
		log.info("Fetching activity log"); // Add logging
		model.addAttribute("activityLogs", activityLogService.getAllLogs());
		return "admin/activity-log"; // Renders activity-log.html
	}
}