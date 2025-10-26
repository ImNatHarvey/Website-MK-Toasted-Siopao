package com.toastedsiopao.controller;

import com.toastedsiopao.dto.CategoryDto; // Import CategoryDto
import com.toastedsiopao.dto.ProductDto; // Import ProductDto
import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.service.ActivityLogService; // Import ActivityLogService
import com.toastedsiopao.service.CategoryService;
import com.toastedsiopao.service.ProductService;
import jakarta.validation.Valid; // Import Valid
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // Import BindingResult
import org.springframework.web.bind.annotation.*; // Import PostMapping, PathVariable, etc.
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Import RedirectAttributes

import java.security.Principal; // Import Principal
import java.util.List;
import java.util.Optional; // Import Optional

@Controller
@RequestMapping("/admin")
public class AdminController {

	// --- Inject Services ---
	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryService categoryService;

	// --- NEW: Inject ActivityLogService ---
	@Autowired
	private ActivityLogService activityLogService;
	// --- End Injection ---

	@GetMapping("/dashboard")
	public String adminDashboard() {
		return "admin/dashboard";
	}

	// --- Product Management (Display List) ---
	@GetMapping("/products")
	public String manageProducts(Model model, @RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "keyword", required = false) String keyword) {

		List<Product> productList;
		List<Category> categoryList = categoryService.findAll(); // Get all categories for filter dropdown

		// --- Basic Filtering/Searching Logic ---
		if (keyword != null && !keyword.isEmpty()) {
			productList = productService.searchProducts(keyword);
			model.addAttribute("keyword", keyword); // Send keyword back to view
		} else if (categoryId != null) {
			productList = productService.findByCategory(categoryId);
			model.addAttribute("selectedCategoryId", categoryId); // Send category ID back to view
		} else {
			productList = productService.findAll(); // Default: show all
		}
		// --- End Filtering ---

		model.addAttribute("products", productList);
		model.addAttribute("categories", categoryList); // For category filter dropdown

		// --- NEW: Add empty DTOs for the "Add" forms ---
		// This ensures the modals have an object to bind to
		if (!model.containsAttribute("productDto")) {
			model.addAttribute("productDto", new ProductDto());
		}
		if (!model.containsAttribute("categoryDto")) {
			model.addAttribute("categoryDto", new CategoryDto());
		}
		// --- End DTO ---

		return "admin/products"; // Return the products template
	}
	// --- End Product Management ---

	// --- NEW: Add Product (POST) ---
	@PostMapping("/products/add")
	public String addProduct(@Valid @ModelAttribute("productDto") ProductDto productDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal) {

		// 1. Check for validation errors
		if (result.hasErrors()) {
			// Add flash attributes to survive redirect
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", result);
			redirectAttributes.addFlashAttribute("productDto", productDto);
			// --- NEW: Add attribute to re-open modal on error ---
			redirectAttributes.addFlashAttribute("showAddProductModal", true);
			return "redirect:/admin/products";
		}

		// 2. If valid, save the product
		try {
			Product savedProduct = productService.save(productDto);
			// Log the action
			activityLogService.logAdminAction(principal.getName(), "ADD_PRODUCT",
					"Added new product: " + savedProduct.getName() + " (ID: " + savedProduct.getId() + ")");
			// Add success message
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + savedProduct.getName() + "' added successfully!");
		} catch (Exception e) {
			// Handle other errors (e.g., category not found)
			redirectAttributes.addFlashAttribute("productError", "Error adding product: " + e.getMessage());
			redirectAttributes.addFlashAttribute("productDto", productDto); // Send DTO back
			redirectAttributes.addFlashAttribute("showAddProductModal", true); // Re-open modal
		}

		return "redirect:/admin/products";
	}
	// --- End Add Product ---

	// --- NEW: Add Category (POST) ---
	@PostMapping("/products/categories/add")
	public String addCategory(@Valid @ModelAttribute("categoryDto") CategoryDto categoryDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal) {

		// 1. Custom check for duplicate category name
		if (categoryDto.getName() != null && !categoryDto.getName().trim().isEmpty()) {
			if (categoryService.findByName(categoryDto.getName().trim()).isPresent()) {
				result.rejectValue("name", "duplicate", "Category name already exists");
			}
		}

		// 2. Check for validation errors
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoryDto", result);
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			// --- NEW: Add attribute to re-open modal on error ---
			redirectAttributes.addFlashAttribute("showCategoryModal", true);
			return "redirect:/admin/products";
		}

		// 3. If valid, save the category
		try {
			Category newCategory = new Category(categoryDto.getName().trim());
			categoryService.save(newCategory);
			// Log the action
			activityLogService.logAdminAction(principal.getName(), "ADD_CATEGORY",
					"Added new category: " + newCategory.getName());
			// Add success message
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + newCategory.getName() + "' added successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("categoryError", "Error adding category: " + e.getMessage());
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showCategoryModal", true);
		}

		return "redirect:/admin/products";
	}
	// --- End Add Category ---

	// --- NEW: Delete Category (POST) ---
	@PostMapping("/products/categories/delete/{id}")
	public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<Category> categoryOpt = categoryService.findById(id);

		if (categoryOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Category not found.");
			return "redirect:/admin/products";
		}

		Category category = categoryOpt.get();

		// 1. Check if category is associated with any products
		if (category.getProducts() != null && !category.getProducts().isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Cannot delete '" + category.getName()
					+ "'. It is associated with " + category.getProducts().size() + " product(s).");
			return "redirect:/admin/products";
		}

		// 2. If not, delete it
		try {
			String categoryName = category.getName();
			categoryService.deleteById(id);
			// Log the action
			activityLogService.logAdminAction(principal.getName(), "DELETE_CATEGORY",
					"Deleted category: " + categoryName + " (ID: " + id + ")");
			// Add success message
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + categoryName + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("categoryError", "Error deleting category: " + e.getMessage());
		}

		return "redirect:/admin/products";
	}
	// --- End Delete Category ---

	@GetMapping("/orders")
	public String manageOrders() {
		return "admin/orders";
	}

	@GetMapping("/customers")
	public String manageCustomers() {
		return "admin/customers";
	}

	@GetMapping("/inventory")
	public String manageInventory() {
		return "admin/inventory";
	}

	@GetMapping("/transactions")
	public String viewTransactions() {
		return "admin/transactions";
	}

	@GetMapping("/settings")
	public String siteSettings() {
		return "admin/settings";
	}

	// --- NEW: Add GET mapping for activity log ---
	@GetMapping("/activity-log")
	public String showActivityLog(Model model) {
		model.addAttribute("activityLogs", activityLogService.getAllLogs());
		return "admin/activity-log";
	}
	// --- End Activity Log ---
}