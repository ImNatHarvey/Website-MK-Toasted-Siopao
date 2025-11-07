package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.service.OrderService;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map; // NEW IMPORT

@Controller
@RequestMapping("/admin/orders") // All methods in this class are under /admin/orders
public class AdminOrderController {

	private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);

	@Autowired
	private OrderService orderService;

	// This now maps to GET /admin/orders
	@GetMapping
	public String manageOrders(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", defaultValue = "0") int page, // NEW
			@RequestParam(value = "size", defaultValue = "10") int size) { // NEW (and default 10)

		log.info("Fetching orders with keyword: '{}', status: '{}', page: {}, size: {}", keyword, status, page, size); // Add
																														// logging

		Pageable pageable = PageRequest.of(page, size); // NEW
		Page<Order> orderPage = orderService.searchOrders(keyword, status, null, null, pageable); // UPDATED

		// --- NEW: Get Order Stats ---
		Map<String, Long> orderStatusCounts = orderService.getOrderStatusCounts();
		long totalOrders = orderStatusCounts.values().stream().mapToLong(Long::longValue).sum();
		model.addAttribute("totalOrders", totalOrders);
		model.addAttribute("pendingOrders", orderStatusCounts.getOrDefault("PENDING", 0L));
		model.addAttribute("processingOrders", orderStatusCounts.getOrDefault("PROCESSING", 0L));
		model.addAttribute("deliveredOrders", orderStatusCounts.getOrDefault("DELIVERED", 0L));
		model.addAttribute("cancelledOrders", orderStatusCounts.getOrDefault("CANCELLED", 0L));
		// --- END NEW ---

		model.addAttribute("orderPage", orderPage); // NEW: Add the full page object
		model.addAttribute("orders", orderPage.getContent()); // UPDATED: Get content from page
		model.addAttribute("keyword", keyword);
		model.addAttribute("currentStatus", status);

		// NEW: Pass pagination attributes to the model
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", orderPage.getTotalPages());
		model.addAttribute("totalItems", orderPage.getTotalElements()); // This is the paged total
		model.addAttribute("size", size);

		return "admin/orders"; // Renders orders.html
	}

	// You can add more order-specific methods here in the future, like:
	// @GetMapping("/{id}")
	// public String viewOrderDetails(@PathVariable("id") Long id, Model model) {
	// ...
	// }

	// @PostMapping("/{id}/updateStatus")
	// public String updateOrderStatus(@PathVariable("id") Long id,
	// @RequestParam("status") String status) {
	// ...
	// }
}