package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order; // NEW IMPORT
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.OrderService; // NEW IMPORT
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // NEW IMPORT
import org.springframework.data.domain.PageRequest; // NEW IMPORT
import org.springframework.data.domain.Pageable; // NEW IMPORT
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // NEW IMPORT

import java.math.BigDecimal; // NEW IMPORT
import java.math.RoundingMode; // NEW IMPORT

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

	@Autowired
	private OrderService orderService; // NEW IMPORT

	@GetMapping("/transactions")
	public String viewTransactions(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		log.info("Accessing transaction history page. Keyword: {}, Page: {}", keyword, page);
		Pageable pageable = PageRequest.of(page, size);

		// Use the OrderService to find all orders (transactions), filtered by keyword
		// We pass 'null' for status to search all statuses
		Page<Order> transactionPage = orderService.searchOrders(keyword, null, pageable);

		// --- Get Metrics ---
		BigDecimal totalRevenue = orderService.getTotalRevenueAllTime();
		long totalTransactions = orderService.getTotalTransactionsAllTime();
		BigDecimal avgOrderValue = BigDecimal.ZERO;
		if (totalTransactions > 0) {
			avgOrderValue = totalRevenue.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP);
		}

		model.addAttribute("transactionPage", transactionPage);
		model.addAttribute("transactions", transactionPage.getContent());
		model.addAttribute("keyword", keyword);

		// --- Add Metrics to Model ---
		model.addAttribute("totalRevenue", totalRevenue);
		model.addAttribute("totalTransactions", totalTransactions);
		model.addAttribute("avgOrderValue", avgOrderValue);

		// --- Add Pagination Attributes ---
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", transactionPage.getTotalPages());
		model.addAttribute("totalItems", transactionPage.getTotalElements());
		model.addAttribute("size", size);

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