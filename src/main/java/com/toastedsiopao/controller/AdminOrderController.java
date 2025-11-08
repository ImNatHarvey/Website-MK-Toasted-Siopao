package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map; 

@Controller
@RequestMapping("/admin/orders") 
public class AdminOrderController {

	private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);

	@Autowired
	private OrderService orderService;

	@GetMapping
	@PreAuthorize("hasAuthority('VIEW_ORDERS')") 
	public String manageOrders(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", defaultValue = "0") int page, 
			@RequestParam(value = "size", defaultValue = "10") int size) { 

		log.info("Fetching orders with keyword: '{}', status: '{}', page: {}, size: {}", keyword, status, page, size);

		Pageable pageable = PageRequest.of(page, size); 
		Page<Order> orderPage = orderService.searchOrders(keyword, status, null, null, pageable);

		Map<String, Long> orderStatusCounts = orderService.getOrderStatusCounts();
		long totalOrders = orderStatusCounts.values().stream().mapToLong(Long::longValue).sum();
		model.addAttribute("totalOrders", totalOrders);
		model.addAttribute("pendingOrders", orderStatusCounts.getOrDefault("PENDING", 0L));
		model.addAttribute("processingOrders", orderStatusCounts.getOrDefault("PROCESSING", 0L));
		model.addAttribute("deliveredOrders", orderStatusCounts.getOrDefault("DELIVERED", 0L));
		model.addAttribute("cancelledOrders", orderStatusCounts.getOrDefault("CANCELLED", 0L));

		model.addAttribute("orderPage", orderPage);
		model.addAttribute("orders", orderPage.getContent()); 
		model.addAttribute("keyword", keyword);
		model.addAttribute("currentStatus", status);

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", orderPage.getTotalPages());
		model.addAttribute("totalItems", orderPage.getTotalElements()); 
		model.addAttribute("size", size);

		return "admin/orders"; 
	}
}