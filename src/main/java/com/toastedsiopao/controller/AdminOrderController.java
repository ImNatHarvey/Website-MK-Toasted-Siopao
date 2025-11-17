package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.service.ActivityLogService; 
import com.toastedsiopao.service.IssueReportService;
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
import org.springframework.web.bind.annotation.PathVariable; 
import org.springframework.web.bind.annotation.PostMapping; 
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; 

import java.security.Principal; 
import java.util.List;
import java.util.Map; 
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/orders") 
public class AdminOrderController {

	private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);

	@Autowired
	private OrderService orderService;

	@Autowired
	private ActivityLogService activityLogService;
	
	@Autowired
	private IssueReportService issueReportService;

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
		model.addAttribute("pendingVerificationOrders", orderStatusCounts.getOrDefault(Order.STATUS_PENDING_VERIFICATION, 0L));
		model.addAttribute("pendingOrders", orderStatusCounts.getOrDefault(Order.STATUS_PENDING, 0L));
		model.addAttribute("processingOrders", orderStatusCounts.getOrDefault(Order.STATUS_PROCESSING, 0L));
		model.addAttribute("outForDeliveryOrders", orderStatusCounts.getOrDefault(Order.STATUS_OUT_FOR_DELIVERY, 0L));
		model.addAttribute("deliveredOrders", orderStatusCounts.getOrDefault(Order.STATUS_DELIVERED, 0L));
		model.addAttribute("cancelledOrders", orderStatusCounts.getOrDefault(Order.STATUS_CANCELLED, 0L));
		model.addAttribute("rejectedOrders", orderStatusCounts.getOrDefault(Order.STATUS_REJECTED, 0L));

		model.addAttribute("orderPage", orderPage);
		model.addAttribute("orders", orderPage.getContent()); 
		model.addAttribute("keyword", keyword);
		model.addAttribute("currentStatus", status);

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", orderPage.getTotalPages());
		model.addAttribute("totalItems", orderPage.getTotalElements()); 
		model.addAttribute("size", size);
		
		List<Long> orderIdsOnPage = orderPage.getContent().stream()
				.map(Order::getId)
				.collect(Collectors.toList());
		
		Map<Long, Long> openIssueCounts = issueReportService.getOpenIssueCountsForOrders(orderIdsOnPage);
		model.addAttribute("openIssueCounts", openIssueCounts);

		return "admin/orders"; 
	}

	@PostMapping("/update-status/{id}")
	@PreAuthorize("hasAuthority('EDIT_ORDERS')")
	public String updateOrderStatus(@PathVariable("id") Long orderId,
									@RequestParam("action") String action,
									Principal principal,
									RedirectAttributes redirectAttributes) {
		
		String adminUsername = principal.getName();
		
		try {
			if ("accept".equals(action)) {
				Order order = orderService.acceptOrder(orderId);
				activityLogService.logAdminAction(adminUsername, "ACCEPT_ORDER", "Accepted Order #ORD-" + orderId + ". Status set to " + order.getStatus());
				redirectAttributes.addFlashAttribute("stockSuccess", "Order #ORD-" + orderId + " accepted.");
			} else if ("reject".equals(action)) {
				Order order = orderService.rejectOrder(orderId);
				activityLogService.logAdminAction(adminUsername, "REJECT_ORDER", "Rejected Order #ORD-" + orderId + ". Stock reversed.");
				redirectAttributes.addFlashAttribute("stockSuccess", "Order #ORD-" + orderId + " rejected. Stock has been reversed.");
			} else if ("ship".equals(action)) {
				Order order = orderService.shipOrder(orderId);
				activityLogService.logAdminAction(adminUsername, "SHIP_ORDER", "Shipped Order #ORD-" + orderId + ". Status set to " + order.getStatus());
				redirectAttributes.addFlashAttribute("stockSuccess", "Order #ORD-" + orderId + " is now Out for Delivery.");
			} else if ("complete_cod".equals(action)) {
				Order order = orderService.completeCodOrder(orderId);
				activityLogService.logAdminAction(adminUsername, "COMPLETE_ORDER", "Completed (COD) Order #ORD-" + orderId + ". Status set to " + order.getStatus());
				redirectAttributes.addFlashAttribute("stockSuccess", "Order #ORD-" + orderId + " has been completed and paid.");
			} else if ("complete_delivered".equals(action)) { 
				Order order = orderService.completeDeliveredOrder(orderId);
				activityLogService.logAdminAction(adminUsername, "COMPLETE_ORDER", "Completed (Pre-Paid) Order #ORD-" + orderId + ". Status set to " + order.getStatus());
				redirectAttributes.addFlashAttribute("stockSuccess", "Order #ORD-" + orderId + " has been marked as delivered.");
			} else {
				throw new IllegalArgumentException("Invalid action.");
			}
		} catch (IllegalArgumentException e) {
			log.warn("Failed to update order status for order #{} by {}: {}", orderId, adminUsername, e.getMessage());
			redirectAttributes.addFlashAttribute("stockError", "Error updating order: " + e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error updating order status for order #{} by {}", orderId, adminUsername, e);
			redirectAttributes.addFlashAttribute("stockError", "An unexpected server error occurred.");
		}

		return "redirect:/admin/orders";
	}
}