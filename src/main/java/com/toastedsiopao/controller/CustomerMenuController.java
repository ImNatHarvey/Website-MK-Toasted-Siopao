package com.toastedsiopao.controller;

import com.toastedsiopao.model.Category; // IMPORTED
import com.toastedsiopao.model.Product; // IMPORTED
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.service.CategoryService; // IMPORTED
import com.toastedsiopao.service.ProductService; // IMPORTED
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // IMPORTED
import org.springframework.data.domain.PageRequest; // IMPORTED
import org.springframework.data.domain.Pageable; // IMPORTED
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // IMPORTED

import java.util.List; // IMPORTED

@Controller
@RequestMapping("/u")
public class CustomerMenuController {

	@Autowired
	private SiteSettingsService siteSettingsService;

	// --- ADDED ---
	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryService categoryService;
	// --- END ADDED ---

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	// --- MODIFIED ---
	@GetMapping("/menu")
	public String customerMenu(Model model, @RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "8") int size) {

		Pageable pageable = PageRequest.of(page, size);
		// --- THIS CALL NOW FILTERS FOR ACTIVE PRODUCTS ---
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

		return "customer/menu";
	}
	// --- END MODIFIED ---
}