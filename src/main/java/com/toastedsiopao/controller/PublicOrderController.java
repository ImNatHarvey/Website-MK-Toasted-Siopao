package com.toastedsiopao.controller;

import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.service.ProductService;
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class PublicOrderController {

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private ProductService productService;

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/order")
	public String order(Model model) {
		Page<Product> productPage = productService.findAll(Pageable.unpaged());
		model.addAttribute("products", productPage.getContent());
		return "order";
	}
}