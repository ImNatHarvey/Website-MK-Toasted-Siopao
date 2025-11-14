package com.toastedsiopao.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toastedsiopao.model.Order; 
import com.toastedsiopao.service.AdminService;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.InventoryItemService;
import com.toastedsiopao.service.OrderService;
import com.toastedsiopao.service.ProductService; 
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
import java.util.ArrayList; // --- ADDED ---
import java.util.LinkedHashMap; // --- ADDED ---
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
	
	@Autowired
	private ProductService productService;

	// --- ADDED: Helper to format chart data ---
	private Map<String, List<?>> getFormattedOrderStatusData(Map<String, Long> statusCounts) {
		// Define the exact order and labels for the chart
		Map<String, String> orderedLabels = new LinkedHashMap<>();
		orderedLabels.put(Order.STATUS_PENDING_VERIFICATION, "PENDING (GCASH)");
		orderedLabels.put(Order.STATUS_PENDING, "PENDING (COD)");
		orderedLabels.put(Order.STATUS_PROCESSING, "PROCESSING");
		orderedLabels.put(Order.STATUS_OUT_FOR_DELIVERY, "OUT FOR DELIVERY");
		orderedLabels.put(Order.STATUS_DELIVERED, "DELIVERED");
		orderedLabels.put(Order.STATUS_CANCELLED, "CANCELLED");
		orderedLabels.put(Order.STATUS_REJECTED, "REJECTED");

		List<String> chartLabels = new ArrayList<>();
		List<Long> chartData = new ArrayList<>();

		// Iterate in the defined order to ensure colors match the JS file
		for (Map.Entry<String, String> entry : orderedLabels.entrySet()) {
			String statusKey = entry.getKey();
			String statusLabel = entry.getValue();
			Long count = statusCounts.getOrDefault(statusKey, 0L);
			
			chartLabels.add(statusLabel);
			chartData.add(count);
		}
		
		return Map.of("labels", chartLabels, "data", chartData);
	}
	// --- END HELPER ---

	@GetMapping("/dashboard")
	@PreAuthorize("hasAuthority('VIEW_DASHBOARD')") 
	public String adminDashboard(Model model) {
		log.info("Loading admin dashboard...");

		// --- Sales Summary ---
		model.addAttribute("salesToday", orderService.getSalesToday());
		model.addAttribute("salesThisWeek", orderService.getSalesThisWeek());
		model.addAttribute("salesThisMonth", orderService.getSalesThisMonth());

		// --- Order Summary ---
		Map<String, Long> orderStatusCounts = orderService.getOrderStatusCounts();
		model.addAttribute("totalOrders", orderStatusCounts.values().stream().mapToLong(Long::longValue).sum());
		model.addAttribute("pendingVerificationOrders", orderStatusCounts.getOrDefault(Order.STATUS_PENDING_VERIFICATION, 0L));
		model.addAttribute("pendingOrders", orderStatusCounts.getOrDefault(Order.STATUS_PENDING, 0L));
		model.addAttribute("processingOrders", orderStatusCounts.getOrDefault(Order.STATUS_PROCESSING, 0L));
		model.addAttribute("outForDeliveryOrders", orderStatusCounts.getOrDefault(Order.STATUS_OUT_FOR_DELIVERY, 0L));
		model.addAttribute("deliveredOrders", orderStatusCounts.getOrDefault(Order.STATUS_DELIVERED, 0L));
		model.addAttribute("cancelledOrders", orderStatusCounts.getOrDefault(Order.STATUS_CANCELLED, 0L));
		model.addAttribute("rejectedOrders", orderStatusCounts.getOrDefault(Order.STATUS_REJECTED, 0L));

		// --- Inventory Summary ---
		model.addAttribute("totalInventoryItems", inventoryItemService.findAll().size()); 
		model.addAttribute("totalStockQuantity", inventoryItemService.getTotalStockQuantity()); 
		model.addAttribute("totalStockValue", inventoryItemService.getTotalStockValue());
		model.addAttribute("lowStockItems", inventoryItemService.countLowStockItems());
		model.addAttribute("criticalStockItems", inventoryItemService.countCriticalStockItems());
		model.addAttribute("outOfStockItems", inventoryItemService.countOutOfStockItems());
		
		// --- PRODUCT SUMMARY (NEW) ---
		model.addAttribute("totalProducts", productService.countAllProducts());
		model.addAttribute("lowStockProducts", productService.countLowStockProducts());
		model.addAttribute("outOfStockProducts", productService.countOutOfStockProducts());
		// --- END PRODUCT SUMMARY ---

		// --- User Summary ---
		model.addAttribute("totalCustomers", customerService.findAllCustomers(null).getTotalElements());
		model.addAttribute("activeCustomers", customerService.countActiveCustomers());
		model.addAttribute("newCustomersThisMonth", customerService.countNewCustomersThisMonth());
		model.addAttribute("totalAdmins", adminService.countAllAdmins());

		// --- Charts ---
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

			// --- MODIFIED: Use helper to get formatted chart data ---
			Map<String, List<?>> orderStatusChartData = getFormattedOrderStatusData(orderStatusCounts);
			model.addAttribute("orderStatusChartLabels", objectMapper.writeValueAsString(orderStatusChartData.get("labels")));
			model.addAttribute("orderStatusChartData", objectMapper.writeValueAsString(orderStatusChartData.get("data")));
			// --- END MODIFIED ---

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