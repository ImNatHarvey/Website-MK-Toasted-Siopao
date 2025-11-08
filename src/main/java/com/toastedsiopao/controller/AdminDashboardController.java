package com.toastedsiopao.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toastedsiopao.service.AdminService;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.InventoryItemService;
import com.toastedsiopao.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin") 
public class AdminDashboardController {

	private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

	@Autowired
	private OrderService orderService;
	@Autowired
	private InventoryItemService inventoryItemService;
	@Autowired
	private CustomerService customerService;
	@Autowired
	private AdminService adminService;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private Clock clock;

	@GetMapping("/dashboard")
	@PreAuthorize("hasAuthority('VIEW_DASHBOARD')") 
	public String adminDashboard(Model model) {
		log.info("Loading admin dashboard...");

		model.addAttribute("salesToday", orderService.getSalesToday());
		model.addAttribute("salesThisWeek", orderService.getSalesThisWeek());
		model.addAttribute("salesThisMonth", orderService.getSalesThisMonth());

		Map<String, Long> orderStatusCounts = orderService.getOrderStatusCounts();
		model.addAttribute("totalOrders", orderStatusCounts.values().stream().mapToLong(Long::longValue).sum());
		model.addAttribute("pendingOrders", orderStatusCounts.getOrDefault("PENDING", 0L));
		model.addAttribute("processingOrders", orderStatusCounts.getOrDefault("PROCESSING", 0L));
		model.addAttribute("deliveredOrders", orderStatusCounts.getOrDefault("DELIVERED", 0L));
		model.addAttribute("cancelledOrders", orderStatusCounts.getOrDefault("CANCELLED", 0L));

		model.addAttribute("totalInventoryItems", inventoryItemService.findAll().size()); 
		model.addAttribute("totalStockQuantity", inventoryItemService.getTotalStockQuantity()); 
																								
		model.addAttribute("totalStockValue", inventoryItemService.getTotalStockValue());
		model.addAttribute("lowStockItems", inventoryItemService.countLowStockItems());
		model.addAttribute("criticalStockItems", inventoryItemService.countCriticalStockItems());
		model.addAttribute("outOfStockItems", inventoryItemService.countOutOfStockItems());

		model.addAttribute("totalCustomers", customerService.findAllCustomers(null).getTotalElements());
		model.addAttribute("activeCustomers", customerService.countActiveCustomers());
		model.addAttribute("newCustomersThisMonth", customerService.countNewCustomersThisMonth());
		model.addAttribute("totalAdmins", adminService.countAllAdmins());

		try {
			LocalDateTime now = LocalDateTime.now(clock);
			LocalDateTime thirtyDaysAgo = now.minusDays(30).with(LocalTime.MIN);
			Map<String, BigDecimal> salesData = orderService.getSalesDataForChart(thirtyDaysAgo, now);
			model.addAttribute("salesChartLabels", objectMapper.writeValueAsString(salesData.keySet()));
			model.addAttribute("salesChartData", objectMapper.writeValueAsString(salesData.values()));

			List<Map<String, Object>> topProducts = orderService.getTopSellingProducts(5);
			model.addAttribute("topProducts", topProducts); 
			List<String> topProductLabels = topProducts.stream()
					.map(p -> (String) ((com.toastedsiopao.model.Product) p.get("product")).getName()).toList();
			List<Long> topProductData = topProducts.stream().map(p -> (Long) p.get("quantity")).toList();
			model.addAttribute("topProductsChartLabels", objectMapper.writeValueAsString(topProductLabels));
			model.addAttribute("topProductsChartData", objectMapper.writeValueAsString(topProductData));

			model.addAttribute("orderStatusChartLabels", objectMapper.writeValueAsString(orderStatusCounts.keySet()));
			model.addAttribute("orderStatusChartData", objectMapper.writeValueAsString(orderStatusCounts.values()));

		} catch (JsonProcessingException e) {
			log.error("Error serializing chart data to JSON", e);
			model.addAttribute("salesChartLabels", "[]");
			model.addAttribute("salesChartData", "[]");
			model.addAttribute("topProductsChartLabels", "[]");
			model.addAttribute("topProductsChartData", "[]");
			model.addAttribute("orderStatusChartLabels", "[]");
			model.addAttribute("orderStatusChartData", "[]");
			model.addAttribute("topProducts", List.of());
		}

		return "admin/dashboard"; 
	}
}