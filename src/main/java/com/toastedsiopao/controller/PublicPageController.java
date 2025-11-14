package com.toastedsiopao.controller;

import com.toastedsiopao.model.CartItem; 
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User; 
import com.toastedsiopao.service.CartService; 
import com.toastedsiopao.service.CustomerService; 
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal; 
import java.security.Principal; 
import java.util.List; 

@Controller
public class PublicPageController {

	@Autowired
	private SiteSettingsService siteSettingsService;

	// --- REMOVED: CustomerService and CartService (now in GlobalModelAttributes) ---

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/")
	public String home(Model model, Principal principal) { 
		
		// --- REMOVED: All cart-loading logic is now in GlobalModelAttributes ---

		return "index";
	}

	@GetMapping("/about")
	public String about() {
		return "about";
	}

	@GetMapping("/access-denied")
	public String accessDenied() {
		return "access-denied";
	}
}