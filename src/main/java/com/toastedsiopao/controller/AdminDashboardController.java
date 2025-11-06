package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.OrderService;
import com.toastedsiopao.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/admin") // Base path for general admin pages
public class AdminDashboardController {

	private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

	// --- NEWLY INJECTED SERVICES ---
	@Autowired
	private OrderService orderService;
	@Autowired
	private ProductService productService;
	@Autowired
	private CustomerService customerService;
	// --- END NEW INJECTIONS ---

	@Autowired
	private ActivityLogService activityLogService;

	@GetMapping("/dashboard")
	public String adminDashboard(Model model) {
		// --- NEW: Fetch real stats for the dashboard ---
		Pageable recentOrdersPageable = PageRequest.of(0, 5); // Get top 5 recent orders
		Page<Order> recentOrders = orderService.searchOrders(null, null, recentOrdersPageable);

		// Get top 5 pending orders
		Page<Order> pendingOrders = orderService.searchOrders(null, "PENDING", recentOrdersPageable);

		// Get stats from other services
		long lowStockProducts = productService.countLowStockProducts();
		long outOfStockProducts = productService.countOutOfStockProducts();
		long activeCustomers = customerService.countActiveCustomers();
		long totalProducts = productService.countAllProducts();

		// Add stats to the model
		model.addAttribute("recentOrders", recentOrders.getContent());
		model.addAttribute("pendingOrderCount", pendingOrders.getTotalElements());
		model.addAttribute("lowStockProducts", lowStockProducts);
		model.addAttribute("outOfStockProducts", outOfStockProducts);
		model.addAttribute("activeCustomers", activeCustomers);
		model.addAttribute("totalProducts", totalProducts);

		log.info("Admin dashboard loaded. Pending Orders: {}, Low Stock: {}", pendingOrders.getTotalElements(),
				lowStockProducts);
		// --- END NEW STATS LOGIC ---

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