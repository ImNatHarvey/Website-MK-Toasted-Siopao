package com.toastedsiopao.controller;

import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/u")
public class CustomerDashboardController {

	@Autowired
	private SiteSettingsService siteSettingsService;

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/dashboard")
	public String customerDashboard() {
		return "customer/dashboard";
	}
}