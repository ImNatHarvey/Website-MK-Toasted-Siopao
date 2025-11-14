package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.NotificationService; // --- ADDED ---
import com.toastedsiopao.service.OrderService;
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable; 
import org.springframework.web.bind.annotation.PostMapping; 
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; 

import java.security.Principal;

@Controller
@RequestMapping("/u")
public class CustomerHistoryController {

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private OrderService orderService;
	
	// --- ADDED ---
	@Autowired
	private NotificationService notificationService;
	// --- END ADDED ---

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/history")
	public String customerHistory(Model model, Principal principal,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "5") int size) { 

		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return "redirect:/logout";
		}

		Pageable pageable = PageRequest.of(page, size);
		Page<Order> orderPage = orderService.findOrdersByUserAndStatus(user, status, pageable);

		model.addAttribute("orderPage", orderPage);
		model.addAttribute("currentStatus", status);

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", orderPage.getTotalPages());
		model.addAttribute("totalItems", orderPage.getTotalElements());
		model.addAttribute("size", size);

		return "customer/history";
	}

	@PostMapping("/history/cancel/{id}")
	public String cancelOrder(@PathVariable("id") Long orderId, Principal principal,
			RedirectAttributes redirectAttributes) {

		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return "redirect:/logout";
		}

		try {
			orderService.cancelOrder(orderId, user);
			redirectAttributes.addFlashAttribute("orderSuccess", "Order #ORD-" + orderId + " has been cancelled.");
			
			// --- ADDED: Admin Notification ---
			String notifMessage = "Customer " + user.getUsername() + " cancelled order #" + orderId + ". Stock has been reversed.";
			String notifLink = "/admin/orders?status=CANCELLED";
			notificationService.createAdminNotification(notifMessage, notifLink);
			// --- END ADDED ---
			
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("orderError", e.getMessage());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("orderError", "An unexpected error occurred. Please try again.");
		}

		return "redirect:/u/history";
	}
}