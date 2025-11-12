package com.toastedsiopao.controller;

import org.springframework.beans.factory.annotation.Autowired; // --- RE-ADDED ---
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // --- RE-ADDED ---
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute; // --- RE-ADDED ---
import org.springframework.web.bind.annotation.RequestMapping;

import com.toastedsiopao.model.SiteSettings; // --- RE-ADDED ---
import com.toastedsiopao.service.SiteSettingsService; // --- RE-ADDED ---

@Controller
@RequestMapping("/u")
public class CustomerController {

	// --- RE-ADDED: Inject SiteSettingsService ---
	@Autowired
	private SiteSettingsService siteSettingsService;

	// --- RE-ADDED: Common method to add site settings to all /u/ pages ---
	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/dashboard")
	public String customerDashboard() {
		return "customer/dashboard";
	}

	@GetMapping("/profile")
	public String customerProfile() {
		return "customer/profile";
	}

	@GetMapping("/history")
	public String customerHistory() {
		return "customer/history";
	}

	@GetMapping("/menu")
	public String customerMenu() {
		return "customer/menu";
	}

	@GetMapping("/order")
	public String customerOrder() {
		return "customer/order";
	}
}