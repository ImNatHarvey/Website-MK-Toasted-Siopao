package com.toastedsiopao.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toastedsiopao.dto.CategoryDto;
import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.CategoryService;
import com.toastedsiopao.service.InventoryItemService;
import com.toastedsiopao.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError; // Import FieldError
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

	private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

	@Autowired
	private ProductService productService;
	@Autowired
	private CategoryService categoryService;
	@Autowired
	private InventoryItemService inventoryItemService;
	@Autowired
	private ActivityLogService activityLogService;
	@Autowired
	private ObjectMapper objectMapper;

	// Helper to add common attributes for redirects
	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("categories", categoryService.findAll());
		redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAll());
		// We might need the JSON map again too, but it's complex for a redirect
		// For now, let's rely on the GET mapping to rebuild it.
	}

	@GetMapping // Unchanged
	public String manageProducts(Model model, @RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "keyword", required = false) String keyword) {
		// ... (logic unchanged) ...
		List<Product> productList;
		List<Category> categoryList = categoryService.findAll();
		List<InventoryItem> inventoryItems = inventoryItemService.findAll();

		if (keyword != null && !keyword.isEmpty()) {
			productList = productService.searchProducts(keyword);
			model.addAttribute("keyword", keyword);
		} else if (categoryId != null) {
			productList = productService.findByCategory(categoryId);
			model.addAttribute("selectedCategoryId", categoryId);
		} else {
			productList = productService.findAll();
		}

		model.addAttribute("products", productList);
		model.addAttribute("categories", categoryList);
		model.addAttribute("inventoryItems", inventoryItems);
		Map<Long, BigDecimal> inventoryStockMap = new HashMap<>();
		for (InventoryItem item : inventoryItems) {
			inventoryStockMap.put(item.getId(), item.getCurrentStock());
		}
		String inventoryStockMapJson = "{}";
		try {
			inventoryStockMapJson = objectMapper.writeValueAsString(inventoryStockMap);
		} catch (JsonProcessingException e) {
			log.error("Error converting inventory stock map to JSON", e);
		}
		model.addAttribute("inventoryStockMapJson", inventoryStockMapJson);
		if (!model.containsAttribute("productDto")) {
			model.addAttribute("productDto", new ProductDto());
		}
		if (!model.containsAttribute("categoryDto")) {
			model.addAttribute("categoryDto", new CategoryDto());
		}
		if (!model.containsAttribute("productUpdateDto")) {
			model.addAttribute("productUpdateDto", new ProductDto());
		}
		return "admin/products";
	}

	@PostMapping("/add")
	public String addProduct(@Valid @ModelAttribute("productDto") ProductDto productDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		// --- Removed Threshold Check ---

		// Check DTO validation errors first
		if (result.hasErrors()) {
			log.warn("Product DTO validation failed for add. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", result);
			redirectAttributes.addFlashAttribute("productDto", productDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "addProductModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles threshold validation
			Product savedProduct = productService.save(productDto);
			activityLogService.logAdminAction(principal.getName(), "ADD_PRODUCT",
					"Added new product: " + savedProduct.getName() + " (ID: " + savedProduct.getId() + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + savedProduct.getName() + "' added successfully!");

		} catch (RuntimeException e) { // Catch validation (threshold) or other runtime errors
			log.warn("Error adding product: {}", e.getMessage(), e);
			// Add specific error back to BindingResult or use a generic flash attribute
			if (e.getMessage().contains("threshold")) {
				result.reject("global", e.getMessage()); // Use global error for threshold issue
			} else {
				redirectAttributes.addFlashAttribute("productError", "Error adding product: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", result);
			redirectAttributes.addFlashAttribute("productDto", productDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "addProductModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/products";
	}

	@PostMapping("/update")
	public String updateProduct(@Valid @ModelAttribute("productUpdateDto") ProductDto productDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		// --- Removed Threshold Check ---

		// Check DTO validation errors first
		if (result.hasErrors()) {
			log.warn("Product DTO validation failed for update. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("productUpdateDto", productDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "editProductModal")
					.queryParam("editId", productDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			if (productDto.getId() == null)
				throw new IllegalArgumentException("Product ID is missing for update.");
			// Service method now handles threshold validation
			Product savedProduct = productService.save(productDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_PRODUCT",
					"Updated product: " + savedProduct.getName() + " (ID: " + savedProduct.getId() + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + savedProduct.getName() + "' updated successfully!");

		} catch (RuntimeException e) { // Catch validation (threshold) or other runtime errors
			log.warn("Error updating product: {}", e.getMessage(), e);
			if (e.getMessage().contains("threshold")) {
				result.reject("global", e.getMessage()); // Use global error for threshold issue
			} else {
				redirectAttributes.addFlashAttribute("productError", "Error updating product: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("productUpdateDto", productDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "editProductModal")
					.queryParam("editId", productDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/products";
	}

	@PostMapping("/categories/add")
	public String addCategory(@Valid @ModelAttribute("categoryDto") CategoryDto categoryDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		// --- Removed manual duplicate name check ---

		// Check DTO validation errors first
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoryDto", result);
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes); // Might need products list too
			redirectAttributes.addFlashAttribute("products", productService.findAll()); // Add products list back
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles validation and saving
			Category newCategory = categoryService.saveFromDto(categoryDto);
			activityLogService.logAdminAction(principal.getName(), "ADD_CATEGORY",
					"Added new product category: " + newCategory.getName());
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + newCategory.getName() + "' added successfully!");

		} catch (IllegalArgumentException e) { // Catch validation errors from service
			log.warn("Validation error adding product category: {}", e.getMessage());
			if (e.getMessage().contains("already exists")) {
				result.rejectValue("name", "duplicate", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("categoryError", "Error adding category: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoryDto", result);
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			redirectAttributes.addFlashAttribute("products", productService.findAll());
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (RuntimeException e) { // Catch other runtime errors
			log.error("Error adding product category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			redirectAttributes.addFlashAttribute("products", productService.findAll());
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/products";
	}

	// ... (Stock Adjust, Delete Product, Delete Category methods remain unchanged)
	// ...
	@PostMapping("/stock/adjust")
	public String adjustProductStock(@RequestParam("productId") Long productId,
			@RequestParam("quantityChange") int quantityChange, RedirectAttributes redirectAttributes,
			Principal principal, UriComponentsBuilder uriBuilder) {
		// ... (unchanged) ...
		String derivedReason = quantityChange > 0 ? "Production" : "Adjustment/Wastage";
		if (quantityChange == 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity change cannot be zero.");
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		try {
			Product updatedProduct = productService.adjustStock(productId, quantityChange, derivedReason);
			String action = quantityChange > 0 ? "Produced" : "Adjusted";
			String details = action + " " + Math.abs(quantityChange) + " units of " + updatedProduct.getName()
					+ " (ID: " + productId + "). Reason: " + derivedReason;
			redirectAttributes.addFlashAttribute("stockSuccess", action + " " + Math.abs(quantityChange) + " units of '"
					+ updatedProduct.getName() + "'. New stock: " + updatedProduct.getCurrentStock());
			activityLogService.logAdminAction(principal.getName(), "ADJUST_PRODUCT_STOCK", details);
		} catch (RuntimeException e) {
			log.warn("Stock adjustment failed: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("stockError", "Error adjusting stock: " + e.getMessage());
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/products";
	}

	@PostMapping("/delete/{id}")
	public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		// ... (unchanged) ...
		Optional<Product> productOpt = productService.findById(id);
		if (productOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("productError", "Product not found.");
			return "redirect:/admin/products";
		}
		String productName = productOpt.get().getName();
		try {
			productService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_PRODUCT",
					"Deleted product: " + productName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + productName + "' deleted successfully!");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("productError",
					"Could not delete product '" + productName + "': " + e.getMessage());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("productError", "An unexpected error occurred...");
		}
		return "redirect:/admin/products";
	}

	@PostMapping("/categories/delete/{id}")
	public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		// ... (unchanged) ...
		Optional<Category> categoryOpt = categoryService.findById(id);
		if (categoryOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Category not found.");
			return "redirect:/admin/products";
		}
		Category category = categoryOpt.get();
		List<Product> productsInCategory = productService.findByCategory(id);
		if (!productsInCategory.isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Cannot delete '" + category.getName()
					+ "'. Associated with " + productsInCategory.size() + " product(s).");
			return "redirect:/admin/products";
		}
		try {
			String categoryName = category.getName();
			categoryService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_CATEGORY",
					"Deleted product category: " + categoryName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + categoryName + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("categoryError", "Error deleting category: " + e.getMessage());
		}
		return "redirect:/admin/products";
	}
}