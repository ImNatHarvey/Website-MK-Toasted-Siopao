package com.toastedsiopao.controller;

import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User; // IMPORTED
import com.toastedsiopao.service.CustomerService; // IMPORTED
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal; // IMPORTED

@Controller
@RequestMapping("/u")
public class CustomerOrderController {

	@Autowired
	private SiteSettingsService siteSettingsService;

	// --- ADDED ---
	@Autowired
	private CustomerService customerService;
	// --- END ADDED ---

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	// --- MODIFIED ---
	@GetMapping("/order")
	public String customerOrder(Model model, Principal principal) {
		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return "redirect:/logout";
		}
		model.addAttribute("customer", user);
		return "customer/order";
	}
	// --- END MODIFIED ---
}