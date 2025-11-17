package com.toastedsiopao.controller;

import com.toastedsiopao.model.CartItem; 
import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User; 
import com.toastedsiopao.service.CartService; 
import com.toastedsiopao.service.CategoryService;
import com.toastedsiopao.service.CustomerService; 
import com.toastedsiopao.service.ProductService;
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal; 
import java.security.Principal; 
import java.util.List;

@Controller
public class PublicMenuController {

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryService categoryService;

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/menu")
	public String menu(Model model, @RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "8") int size,
			Principal principal) { 

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
}