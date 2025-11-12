package com.toastedsiopao.controller;

import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.SiteSettings; // --- RE-ADDED ---
import com.toastedsiopao.service.CategoryService;
import com.toastedsiopao.service.ProductService;
import com.toastedsiopao.service.SiteSettingsService; // --- RE-ADDED ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute; // --- RE-ADDED ---
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class StaticPageController {

	@Autowired
	private SiteSettingsService siteSettingsService; // --- RE-ADDED ---

	// --- ADDED: Services for dynamic data ---
	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryService categoryService;

	@ModelAttribute // --- RE-ADDED ---
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/")
	public String home() {
		return "index";
	}

	// --- MODIFIED: Updated to fetch products and categories for the menu ---
	@GetMapping("/menu")
	public String menu(Model model, @RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "8") int size) { // Changed to 8 for a 4-col layout

		Pageable pageable = PageRequest.of(page, size);
		Page<Product> productPage = productService.searchProducts(keyword, categoryId, pageable);
		List<Category> categoryList = categoryService.findAll();

		model.addAttribute("productPage", productPage);
		model.addAttribute("products", productPage.getContent());
		model.addAttribute("categories", categoryList);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategoryId", categoryId);

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", productPage.getTotalPages());
		model.addAttribute("totalItems", productPage.getTotalElements());
		model.addAttribute("size", size);

		return "menu";
	}

	// --- MODIFIED: Updated to fetch products for the order summary ---
	@GetMapping("/order")
	public String order(Model model) {
		// Fetch all products for the order summary (can be optimized later)
		Page<Product> productPage = productService.findAll(Pageable.unpaged());
		model.addAttribute("products", productPage.getContent());
		return "order";
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