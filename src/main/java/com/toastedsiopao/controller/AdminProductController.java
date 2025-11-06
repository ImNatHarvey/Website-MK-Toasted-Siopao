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
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.PageRequest; // Import PageRequest
import org.springframework.data.domain.Pageable; // Import Pageable
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
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page, // NEW
			@RequestParam(value = "size", defaultValue = "8") int size) { // NEW, size 8

		Pageable pageable = PageRequest.of(page, size); // NEW
		Page<Product> productPage; // NEW

		List<Category> categoryList = categoryService.findAll();
		List<InventoryItem> inventoryItems = inventoryItemService.findAll();

		// --- UPDATED Search Logic ---
		log.info("Fetching products with keyword: '{}', categoryId: {}, page: {}, size: {}", keyword, categoryId, page,
				size);
		productPage = productService.searchProducts(keyword, categoryId, pageable);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategoryId", categoryId);
		// --- END UPDATED Logic ---

		model.addAttribute("productPage", productPage); // NEW: Add page object
		model.addAttribute("products", productPage.getContent()); // Get content from page
		model.addAttribute("categories", categoryList);
		model.addAttribute("inventoryItems", inventoryItems);

		// NEW: Pass pagination attributes
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", productPage.getTotalPages());
		model.addAttribute("totalItems", productPage.getTotalElements());
		model.addAttribute("size", size);

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

		// --- NEW: Check for empty ingredients ---
		if (productDto.getIngredients() == null || productDto.getIngredients().isEmpty()) {
			log.warn("Product add failed: No ingredients provided for {}.", productDto.getName());
			// Add an error to the 'ingredients' field in the DTO
			result.rejectValue("ingredients", "NotEmpty", "A product must have at least one ingredient.");
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", result);
			redirectAttributes.addFlashAttribute("productDto", productDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "addProductModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		// --- END NEW ---

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
			redirectAttributes.addFlashAttribute("products", productService.findAll(PageRequest.of(0, 8))); // Add
																											// products
																											// list
																											// back
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
			redirectAttributes.addFlashAttribute("products", productService.findAll(PageRequest.of(0, 8)));
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (RuntimeException e) { // Catch other runtime errors
			log.error("Error adding product category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			redirectAttributes.addFlashAttribute("products", productService.findAll(PageRequest.of(0, 8)));
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/products";
	}

	@PostMapping("/stock/adjust")
	public String adjustProductStock(@RequestParam("productId") Long productId, @RequestParam("quantity") int quantity, // CHANGED
			@RequestParam("action") String action, // ADDED
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		// Validate that quantity is positive
		if (quantity <= 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity must be a positive number.");
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		// Determine the final quantity change (positive or negative)
		int quantityChange = action.equals("deduct") ? -quantity : quantity;
		String derivedReason = action.equals("add") ? "Production" : "Adjustment/Wastage";

		try {
			Product updatedProduct = productService.adjustStock(productId, quantityChange, derivedReason);
			String actionText = action.equals("add") ? "Produced" : "Adjusted";
			String details = actionText + " " + quantity + " units of " + updatedProduct.getName() + " (ID: "
					+ productId + "). Reason: " + derivedReason;
			redirectAttributes.addFlashAttribute("stockSuccess", actionText + " " + quantity + " units of '"
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
		// We must use the paginated method to check
		Page<Product> productsInCategory = productService.findByCategory(id, PageRequest.of(0, 1));
		if (!productsInCategory.isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Cannot delete '" + category.getName()
					+ "'. Associated with " + productsInCategory.getTotalElements() + " product(s).");
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