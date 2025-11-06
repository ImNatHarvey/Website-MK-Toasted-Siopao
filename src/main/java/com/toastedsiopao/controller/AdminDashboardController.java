package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.PageRequest; // Import PageRequest
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin") // Base path for general admin pages
public class AdminDashboardController {

	private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

	@Autowired
	private OrderService orderService;
	@Autowired
	private ActivityLogService activityLogService;

	@GetMapping("/dashboard")
	public String adminDashboard(Model model) {
		// Add logic here later to fetch dashboard stats if needed
		// For now, just return the view name
		return "admin/dashboard"; // Renders dashboard.html
	}

	@GetMapping("/orders")
	public String manageOrders(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", defaultValue = "0") int page, // NEW
			@RequestParam(value = "size", defaultValue = "10") int size) { // NEW (and default 10)

		log.info("Fetching orders with keyword: '{}', status: '{}', page: {}, size: {}", keyword, status, page, size); // Add
																														// logging

		Pageable pageable = PageRequest.of(page, size); // NEW
		Page<Order> orderPage = orderService.searchOrders(keyword, status, pageable); // UPDATED

		model.addAttribute("orderPage", orderPage); // NEW: Add the full page object
		model.addAttribute("orders", orderPage.getContent()); // UPDATED: Get content from page
		model.addAttribute("keyword", keyword);
		model.addAttribute("currentStatus", status);

		// NEW: Pass pagination attributes to the model
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", orderPage.getTotalPages());
		model.addAttribute("totalItems", orderPage.getTotalElements());
		model.addAttribute("size", size);

		return "admin/orders"; // Renders orders.html
	}

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