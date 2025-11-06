package com.toastedsiopao.controller;

import com.toastedsiopao.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to handle general admin pages like Settings, Transactions, and
 * Activity Log, separating them from the main dashboard logic.
 */
@Controller
@RequestMapping("/admin") // All methods in this class are under /admin
public class AdminSiteController {

	private static final Logger log = LoggerFactory.getLogger(AdminSiteController.class);

	@Autowired
	private ActivityLogService activityLogService;

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