package com.toastedsiopao.controller;

import com.toastedsiopao.dto.AdminAdminUpdateDto;
import com.toastedsiopao.dto.AdminCustomerUpdateDto;
import com.toastedsiopao.dto.AdminUserCreateDto;
import com.toastedsiopao.dto.CategoryDto; // Product Category
import com.toastedsiopao.dto.InventoryCategoryDto; // Inventory Category
import com.toastedsiopao.dto.InventoryItemDto; // Inventory Item
import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.dto.RecipeIngredientDto;
import com.toastedsiopao.dto.UnitOfMeasureDto; // Unit of Measure

import com.toastedsiopao.model.Category; // Product Category
import com.toastedsiopao.model.InventoryCategory; // Inventory Category
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.RecipeIngredient;
import com.toastedsiopao.model.UnitOfMeasure; // Unit of Measure
import com.toastedsiopao.model.User;

import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.CategoryService; // Product Category
import com.toastedsiopao.service.InventoryCategoryService; // Inventory Category
import com.toastedsiopao.service.InventoryItemService;
import com.toastedsiopao.service.OrderService;
import com.toastedsiopao.service.ProductService;
import com.toastedsiopao.service.UnitOfMeasureService; // Unit of Measure
import com.toastedsiopao.service.UserService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

	// --- Inject Services ---
	@Autowired
	private ProductService productService;
	@Autowired
	private CategoryService categoryService; // Product Categories
	@Autowired
	private ActivityLogService activityLogService;
	@Autowired
	private UserService userService;
	@Autowired
	private OrderService orderService;
	@Autowired
	private InventoryItemService inventoryItemService; // Inventory Item
	@Autowired
	private InventoryCategoryService inventoryCategoryService;
	@Autowired
	private UnitOfMeasureService unitOfMeasureService;
	@Autowired
	private PasswordEncoder passwordEncoder;
	// --- End Injection ---

	@GetMapping("/dashboard")
	public String adminDashboard() {
		return "admin/dashboard";
	}

	// --- Product Management ---
	@GetMapping("/products")
	public String manageProducts(Model model, @RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "keyword", required = false) String keyword) {

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

		if (!model.containsAttribute("productDto")) {
			model.addAttribute("productDto", new ProductDto());
		}
		if (!model.containsAttribute("categoryDto")) {
			model.addAttribute("categoryDto", new CategoryDto());
		}

		return "admin/products";
	}

	@PostMapping("/products/add")
	public String addProduct(@Valid @ModelAttribute("productDto") ProductDto productDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, Model model) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", result);
			redirectAttributes.addFlashAttribute("productDto", productDto);
			redirectAttributes.addFlashAttribute("showAddProductModal", true);
			redirectAttributes.addFlashAttribute("categories", categoryService.findAll());
			redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAll());
			return "redirect:/admin/products";
		}

		try {
			Product savedProduct = productService.save(productDto);
			activityLogService.logAdminAction(principal.getName(), "ADD_PRODUCT",
					"Added new product: " + savedProduct.getName() + " (ID: " + savedProduct.getId() + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + savedProduct.getName() + "' added successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("productError", "Error adding product: " + e.getMessage());
			redirectAttributes.addFlashAttribute("productDto", productDto);
			redirectAttributes.addFlashAttribute("showAddProductModal", true);
			redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAll());
			redirectAttributes.addFlashAttribute("categories", categoryService.findAll());
		}

		return "redirect:/admin/products";
	}

	@PostMapping("/products/delete/{id}")
	public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<Product> productOpt = productService.findById(id);

		if (productOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("productError", "Product not found. Could not delete.");
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
			redirectAttributes.addFlashAttribute("productError", e.getMessage());
		}

		return "redirect:/admin/products";
	}

	@GetMapping("/products/edit/{id}")
	public String showEditProductForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {

		Optional<Product> productOpt = productService.findById(id);

		if (productOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("productError", "Product not found (ID: " + id + "). Cannot edit.");
			return "redirect:/admin/products";
		}

		Product product = productOpt.get();

		// Populate DTO if not already present (e.g., from a failed validation redirect)
		if (!model.containsAttribute("productDto")) {
			ProductDto productDto = new ProductDto();
			productDto.setId(product.getId());
			productDto.setName(product.getName());
			productDto.setDescription(product.getDescription());
			productDto.setPrice(product.getPrice());
			productDto.setCategoryId(product.getCategory().getId());
			productDto.setImageUrl(product.getImageUrl());
			productDto.setLowStockThreshold(product.getLowStockThreshold());
			productDto.setCriticalStockThreshold(product.getCriticalStockThreshold());

			if (product.getIngredients() != null) {
				for (RecipeIngredient ingredient : product.getIngredients()) {
					if (ingredient.getInventoryItem() != null) {
						RecipeIngredientDto ingredientDto = new RecipeIngredientDto();
						ingredientDto.setInventoryItemId(ingredient.getInventoryItem().getId());
						ingredientDto.setQuantityNeeded(ingredient.getQuantityNeeded());
						productDto.getIngredients().add(ingredientDto);
					}
				}
			}
			model.addAttribute("productDto", productDto);
		}
		// Add necessary data for rendering the form
		model.addAttribute("product", product); // For displaying current stock
		model.addAttribute("categories", categoryService.findAll());
		model.addAttribute("inventoryItems", inventoryItemService.findAll());

		return "admin/edit-product";
	}

	@PostMapping("/products/update")
	public String updateProduct(@Valid @ModelAttribute("productDto") ProductDto productDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, Model model) { // Added Model

		// --- THIS IS THE CHANGE ---
		if (result.hasErrors()) {
			// Add necessary attributes back to the model for re-rendering the form
			model.addAttribute("categories", categoryService.findAll());
			model.addAttribute("inventoryItems", inventoryItemService.findAll());
			// Fetch the product again if ID exists to display current stock
			if (productDto.getId() != null) {
				productService.findById(productDto.getId()).ifPresent(p -> model.addAttribute("product", p));
			}
			// ***Explicitly add the BindingResult with a specific name***
			model.addAttribute("bindingResult", result);
			return "admin/edit-product"; // Return the view name directly
		}
		// --- END CHANGE ---

		try {
			Product savedProduct = productService.save(productDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_PRODUCT",
					"Updated product: " + savedProduct.getName() + " (ID: " + savedProduct.getId() + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + savedProduct.getName() + "' updated successfully!");
			return "redirect:/admin/products"; // Redirect on success
		} catch (Exception e) {
			// Add attributes needed for the form to the redirect attributes
			redirectAttributes.addFlashAttribute("productError", "Error updating product: " + e.getMessage());
			redirectAttributes.addFlashAttribute("productDto", productDto); // Send DTO back
			redirectAttributes.addFlashAttribute("categories", categoryService.findAll());
			redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAll());
			// ***Add BindingResult to redirect attributes as well for consistency, though
			// not used here***
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", result);

			// Redirect back to the GET mapping for the edit page
			return "redirect:/admin/products/edit/" + productDto.getId();
		}
	}

	@PostMapping("/products/stock/adjust")
	public String adjustProductStock(@RequestParam("productId") Long productId,
			@RequestParam("quantityChange") int quantityChange, @RequestParam("reason") String reason,
			RedirectAttributes redirectAttributes, Principal principal) {

		if (!StringUtils.hasText(reason)) {
			redirectAttributes.addFlashAttribute("stockError", "Reason is required for stock adjustment.");
			redirectAttributes.addFlashAttribute("showManageStockModal", true);
			return "redirect:/admin/products";
		}

		if (quantityChange == 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity change cannot be zero.");
			redirectAttributes.addFlashAttribute("showManageStockModal", true);
			return "redirect:/admin/products";
		}

		try {
			Product updatedProduct = productService.adjustStock(productId, quantityChange, reason);
			String action = quantityChange > 0 ? "Produced" : "Removed";
			String details = action + " " + Math.abs(quantityChange) + " units of " + updatedProduct.getName()
					+ " (ID: " + productId + "). Reason: " + reason;

			redirectAttributes.addFlashAttribute("stockSuccess", action + " " + Math.abs(quantityChange) + " units of '"
					+ updatedProduct.getName() + "'. New stock: " + updatedProduct.getCurrentStock());

			activityLogService.logAdminAction(principal.getName(), "ADJUST_PRODUCT_STOCK", details);

		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("stockError", "Error adjusting stock: " + e.getMessage());
			redirectAttributes.addFlashAttribute("showManageStockModal", true);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("stockError", "An unexpected error occurred during stock adjustment.");
			redirectAttributes.addFlashAttribute("showManageStockModal", true);
		}

		return "redirect:/admin/products";
	}

	@GetMapping("/products/view/{id}")
	public String viewProduct(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		Optional<Product> productOpt = productService.findById(id);

		if (productOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("productError", "Product not found (ID: " + id + "). Cannot view.");
			return "redirect:/admin/products";
		}

		model.addAttribute("product", productOpt.get());
		return "admin/view-product";
	}

	@PostMapping("/products/categories/add")
	public String addCategory(@Valid @ModelAttribute("categoryDto") CategoryDto categoryDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal) {
		if (categoryDto.getName() != null && !categoryDto.getName().trim().isEmpty()) {
			if (categoryService.findByName(categoryDto.getName().trim()).isPresent()) {
				result.rejectValue("name", "duplicate", "Category name already exists");
			}
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoryDto", result);
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showCategoryModal", true);
			redirectAttributes.addFlashAttribute("categories", categoryService.findAll());
			return "redirect:/admin/products";
		}

		try {
			Category newCategory = new Category(categoryDto.getName().trim());
			categoryService.save(newCategory);
			activityLogService.logAdminAction(principal.getName(), "ADD_CATEGORY",
					"Added new product category: " + newCategory.getName());
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + newCategory.getName() + "' added successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("categoryError", "Error adding category: " + e.getMessage());
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showCategoryModal", true);
			redirectAttributes.addFlashAttribute("categories", categoryService.findAll());
		}
		return "redirect:/admin/products";
	}

	@PostMapping("/products/categories/delete/{id}")
	public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<Category> categoryOpt = categoryService.findById(id);

		if (categoryOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Category not found.");
			return "redirect:/admin/products";
		}

		Category category = categoryOpt.get();

		if (category.getProducts() != null && !category.getProducts().isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Cannot delete '" + category.getName()
					+ "'. It is associated with " + category.getProducts().size() + " product(s).");
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
	// --- End Product Management ---

	// --- Order Management ---
	@GetMapping("/orders")
	public String manageOrders(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "status", required = false) String status) {
		List<Order> orders = orderService.searchOrders(keyword, status);
		model.addAttribute("orders", orders);
		model.addAttribute("keyword", keyword);
		model.addAttribute("currentStatus", status);
		return "admin/orders";
	}
	// --- End Order Management ---

	// --- Customer/User Management ---
	@GetMapping("/customers")
	public String manageCustomers(Model model, Principal principal,
			@RequestParam(value = "keyword", required = false) String keyword) {
		List<User> customers;
		if (StringUtils.hasText(keyword)) {
			customers = userService.searchCustomers(keyword);
			model.addAttribute("keyword", keyword);
		} else {
			customers = userService.findAllCustomers();
		}
		model.addAttribute("customers", customers);

		model.addAttribute("admins", userService.findAllAdmins());
		model.addAttribute("currentUsername", principal.getName());

		if (!model.containsAttribute("adminUserDto")) {
			model.addAttribute("adminUserDto", new AdminUserCreateDto());
		}
		if (!model.containsAttribute("customerUserDto")) {
			model.addAttribute("customerUserDto", new AdminUserCreateDto());
		}

		// This is for the *new* edit admin modal
		if (!model.containsAttribute("adminUpdateDto")) {
			model.addAttribute("adminUpdateDto", new AdminAdminUpdateDto());
		}

		// This is for the *new* edit customer modal
		if (!model.containsAttribute("customerUpdateDto")) {
			model.addAttribute("customerUpdateDto", new AdminCustomerUpdateDto());
		}

		return "admin/customers";
	}

	// --- THIS METHOD IS NOW OBSOLETE ---
	// @GetMapping("/customers/admin/edit/{id}")
	// public String showEditAdminForm(@PathVariable("id") Long id, Model model,
	// RedirectAttributes redirectAttributes) {
	// Optional<User> userOpt = userService.findUserById(id);

	// if (userOpt.isEmpty() || !userOpt.get().getRole().equals("ROLE_ADMIN")) {
	// redirectAttributes.addFlashAttribute("customerError", "Admin not found (ID: "
	// + id + ").");
	// return "redirect:/admin/customers";
	// }

	// User user = userOpt.get();

	// if (!model.containsAttribute("adminUpdateDto")) {
	// AdminAdminUpdateDto dto = new AdminAdminUpdateDto();
	// dto.setId(user.getId());
	// dto.setFirstName(user.getFirstName());
	// dto.setLastName(user.getLastName());
	// dto.setUsername(user.getUsername());
	// model.addAttribute("adminUpdateDto", dto);
	// }

	// return "admin/edit-admin"; // This file will be deleted
	// }

	@PostMapping("/customers/add-admin")
	public String addAdminUser(@Valid @ModelAttribute("adminUserDto") AdminUserCreateDto userDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal) {

		if (userService.findByUsername(userDto.getUsername()) != null) {
			result.rejectValue("username", "userDto.username", "Username already exists");
		}

		if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "userDto.confirmPassword", "Passwords do not match");
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUserDto", result);
			redirectAttributes.addFlashAttribute("adminUserDto", userDto);
			redirectAttributes.addFlashAttribute("showManageAdminsModal", true);
			return "redirect:/admin/customers";
		}

		try {
			User savedUser = userService.saveAdminUser(userDto, "ROLE_ADMIN");
			activityLogService.logAdminAction(principal.getName(), "ADD_USER (ADMIN)",
					"Created new admin user: " + savedUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Admin user '" + savedUser.getUsername() + "' created successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error creating admin: " + e.getMessage());
			redirectAttributes.addFlashAttribute("adminUserDto", userDto);
			redirectAttributes.addFlashAttribute("showManageAdminsModal", true);
		}

		return "redirect:/admin/customers";
	}

	@PostMapping("/customers/add-customer")
	public String addCustomerUser(@Valid @ModelAttribute("customerUserDto") AdminUserCreateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (userService.findByUsername(userDto.getUsername()) != null) {
			result.rejectValue("username", "userDto.username", "Username already exists");
		}

		if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "userDto.confirmPassword", "Passwords do not match");
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUserDto",
					result);
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			redirectAttributes.addFlashAttribute("showAddCustomerModal", true);
			return "redirect:/admin/customers";
		}

		try {
			User savedUser = userService.saveAdminUser(userDto, "ROLE_CUSTOMER");
			activityLogService.logAdminAction(principal.getName(), "ADD_USER (CUSTOMER)",
					"Created new customer user: " + savedUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer user '" + savedUser.getUsername() + "' created successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error creating customer: " + e.getMessage());
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			redirectAttributes.addFlashAttribute("showAddCustomerModal", true);
		}

		return "redirect:/admin/customers";
	}

	@PostMapping("/customers/update")
	public String updateCustomer(@Valid @ModelAttribute("customerUpdateDto") AdminCustomerUpdateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal, Model model) {

		// --- MODIFIED ERROR HANDLING ---
		if (result.hasErrors()) {
			// Add flash attributes for redirect
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			// Add flag to re-open the *edit* modal
			redirectAttributes.addFlashAttribute("showEditCustomerModal", true);
			return "redirect:/admin/customers";
		}

		try {
			User updatedUser = userService.updateCustomer(userDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_USER (CUSTOMER)",
					"Updated customer user: " + updatedUser.getUsername() + " (ID: " + updatedUser.getId() + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer '" + updatedUser.getUsername() + "' updated successfully!");
			return "redirect:/admin/customers";
		} catch (IllegalArgumentException e) {
			// Handle duplicate username error
			result.rejectValue("username", "userDto.username", e.getMessage());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			redirectAttributes.addFlashAttribute("showEditCustomerModal", true);
			return "redirect:/admin/customers";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error updating customer: " + e.getMessage());
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			redirectAttributes.addFlashAttribute("showEditCustomerModal", true);
			return "redirect:/admin/customers";
		}
		// --- END MODIFICATION ---
	}

	@PostMapping("/customers/admin/update")
	public String updateAdmin(@Valid @ModelAttribute("adminUpdateDto") AdminAdminUpdateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal, Model model) {

		// --- MODIFIED ERROR HANDLING ---
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUpdateDto", result);
			redirectAttributes.addFlashAttribute("adminUpdateDto", userDto);
			// Flag to re-open the *edit admin* modal
			redirectAttributes.addFlashAttribute("showEditAdminModal", true);
			return "redirect:/admin/customers";
		}

		try {
			User updatedUser = userService.updateAdmin(userDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_USER (ADMIN)",
					"Updated admin user: " + updatedUser.getUsername() + " (ID: " + updatedUser.getId() + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Admin '" + updatedUser.getUsername() + "' updated successfully!");
			return "redirect:/admin/customers";
		} catch (IllegalArgumentException e) {
			// Handle duplicate username
			result.rejectValue("username", "userDto.username", e.getMessage());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUpdateDto", result);
			redirectAttributes.addFlashAttribute("adminUpdateDto", userDto);
			redirectAttributes.addFlashAttribute("showEditAdminModal", true);
			return "redirect:/admin/customers";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error updating admin: " + e.getMessage()); // Changed
																												// message
																												// key
			redirectAttributes.addFlashAttribute("adminUpdateDto", userDto);
			redirectAttributes.addFlashAttribute("showEditAdminModal", true);
			return "redirect:/admin/customers";
		}
		// --- END MODIFICATION ---
	}

	@PostMapping("/customers/delete/{id}")
	public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Principal principal) {
		Optional<User> userOpt = userService.findUserById(id);
		if (userOpt.isEmpty() || !userOpt.get().getRole().equals("ROLE_CUSTOMER")) {
			redirectAttributes.addFlashAttribute("customerError", "Customer not found. Could not delete.");
			return "redirect:/admin/customers";
		}

		String username = userOpt.get().getUsername();

		try {
			userService.deleteUserById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_USER (CUSTOMER)",
					"Deleted customer user: " + username + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer '" + username + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error deleting customer: " + e.getMessage());
		}

		return "redirect:/admin/customers";
	}

	@PostMapping("/customers/admin/delete/{id}")
	public String deleteAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Principal principal) {
		Optional<User> userOpt = userService.findUserById(id);
		if (userOpt.isEmpty() || !userOpt.get().getRole().equals("ROLE_ADMIN")) {
			redirectAttributes.addFlashAttribute("customerError", "Admin not found. Could not delete.");
			return "redirect:/admin/customers";
		}

		String username = userOpt.get().getUsername();

		if (principal.getName().equals(username)) {
			redirectAttributes.addFlashAttribute("customerError", "You cannot delete your own account.");
			return "redirect:/admin/customers";
		}

		try {
			userService.deleteUserById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_USER (ADMIN)",
					"Deleted admin user: " + username + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("customerSuccess", "Admin '" + username + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error deleting admin: " + e.getMessage());
		}

		return "redirect:/admin/customers";
	}
	// --- End Customer/User Management ---

	// --- Inventory Management ---
	@GetMapping("/inventory")
	public String manageInventory(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) Long categoryId) {
		List<InventoryItem> items = inventoryItemService.searchItems(keyword, categoryId);
		List<InventoryCategory> categories = inventoryCategoryService.findAll();
		List<UnitOfMeasure> units = unitOfMeasureService.findAll();

		List<InventoryItem> allItems = inventoryItemService.findAll();
		model.addAttribute("allInventoryItems", allItems);

		model.addAttribute("inventoryItems", items);
		model.addAttribute("inventoryCategories", categories);
		model.addAttribute("unitsOfMeasure", units);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategoryId", categoryId);

		if (!model.containsAttribute("inventoryItemDto")) {
			model.addAttribute("inventoryItemDto", new InventoryItemDto());
		}
		if (!model.containsAttribute("inventoryCategoryDto")) {
			model.addAttribute("inventoryCategoryDto", new InventoryCategoryDto());
		}
		if (!model.containsAttribute("unitOfMeasureDto")) {
			model.addAttribute("unitOfMeasureDto", new UnitOfMeasureDto());
		}
		return "admin/inventory";
	}

	@PostMapping("/inventory/save")
	public String saveInventoryItem(@Valid @ModelAttribute("inventoryItemDto") InventoryItemDto itemDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryItemDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			redirectAttributes.addFlashAttribute("showAddItemModal", true);
			return "redirect:/admin/inventory";
		}

		try {
			InventoryItem savedItem = inventoryItemService.save(itemDto);
			String action = itemDto.getId() == null ? "ADD_INVENTORY_ITEM" : "EDIT_INVENTORY_ITEM";
			String message = itemDto.getId() == null ? "Added" : "Updated";

			activityLogService.logAdminAction(principal.getName(), action,
					message + " inventory item: " + savedItem.getName() + " (ID: " + savedItem.getId() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Item '" + savedItem.getName() + "' " + message.toLowerCase() + " successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error saving item: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			redirectAttributes.addFlashAttribute("showAddItemModal", true);
		}

		return "redirect:/admin/inventory";
	}

	@PostMapping("/inventory/delete/{id}")
	public String deleteInventoryItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		Optional<InventoryItem> itemOpt = inventoryItemService.findById(id);
		if (itemOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Item not found. Could not delete.");
			return "redirect:/admin/inventory";
		}

		String itemName = itemOpt.get().getName();

		try {
			inventoryItemService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_ITEM",
					"Deleted inventory item: " + itemName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess", "Item '" + itemName + "' deleted successfully!");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error: " + e.getMessage());
		}

		return "redirect:/admin/inventory";
	}

	@PostMapping("/inventory/stock/adjust")
	public String adjustInventoryStock(@RequestParam("inventoryItemId") Long itemId,
			@RequestParam("quantityChange") BigDecimal quantityChange, @RequestParam("reason") String reason,
			RedirectAttributes redirectAttributes, Principal principal) {

		if (quantityChange.compareTo(BigDecimal.ZERO) == 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity change cannot be zero.");
			redirectAttributes.addFlashAttribute("showAdjustStockModal", true);
			return "redirect:/admin/inventory";
		}

		if (!StringUtils.hasText(reason)) {
			redirectAttributes.addFlashAttribute("stockError", "Reason is required for stock adjustment.");
			redirectAttributes.addFlashAttribute("showAdjustStockModal", true);
			return "redirect:/admin/inventory";
		}

		try {
			InventoryItem updatedItem = inventoryItemService.adjustStock(itemId, quantityChange, reason);
			String action = quantityChange.compareTo(BigDecimal.ZERO) > 0 ? "Increased" : "Decreased";
			String details = action + " stock for " + updatedItem.getName() + " (ID: " + itemId + ") by "
					+ quantityChange.abs() + ". Reason: " + reason;

			redirectAttributes.addFlashAttribute("stockSuccess", action + " stock for '" + updatedItem.getName()
					+ "' by " + quantityChange.abs() + ". New stock: " + updatedItem.getCurrentStock());

			activityLogService.logAdminAction(principal.getName(), "ADJUST_INVENTORY_STOCK", details);

		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("stockError", "Error adjusting stock: " + e.getMessage());
			redirectAttributes.addFlashAttribute("showAdjustStockModal", true);
		}

		return "redirect:/admin/inventory";
	}

	@PostMapping("/inventory/categories/add")
	public String addInventoryCategory(@Valid @ModelAttribute("inventoryCategoryDto") InventoryCategoryDto categoryDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (categoryDto.getName() != null && !categoryDto.getName().trim().isEmpty()) {
			if (inventoryCategoryService.findByName(categoryDto.getName().trim()).isPresent()) {
				result.rejectValue("name", "duplicate", "Category name already exists");
			}
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryCategoryDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showManageCategoriesModal", true);
			return "redirect:/admin/inventory";
		}

		try {
			InventoryCategory newCategory = new InventoryCategory(categoryDto.getName().trim());
			inventoryCategoryService.save(newCategory);
			activityLogService.logAdminAction(principal.getName(), "ADD_INVENTORY_CATEGORY",
					"Added new inventory category: " + newCategory.getName());
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Inventory Category '" + newCategory.getName() + "' added successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error adding category: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showManageCategoriesModal", true);
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/inventory/categories/delete/{id}")
	public String deleteInventoryCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		Optional<InventoryCategory> categoryOpt = inventoryCategoryService.findById(id);
		if (categoryOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Category not found.");
			return "redirect:/admin/inventory";
		}

		InventoryCategory category = categoryOpt.get();

		if (category.getItems() != null && !category.getItems().isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Cannot delete '" + category.getName()
					+ "'. It is associated with " + category.getItems().size() + " inventory item(s).");
			return "redirect:/admin/inventory";
		}

		try {
			String categoryName = category.getName();
			inventoryCategoryService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_CATEGORY",
					"Deleted inventory category: " + categoryName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Category '" + categoryName + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error deleting category: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/inventory/units/add")
	public String addUnitOfMeasure(@Valid @ModelAttribute("unitOfMeasureDto") UnitOfMeasureDto unitDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (!result.hasFieldErrors("name") && !result.hasFieldErrors("abbreviation")) {
			Optional<UnitOfMeasure> existing = unitOfMeasureService.findByNameOrAbbreviation(unitDto.getName().trim(),
					unitDto.getAbbreviation().trim());
			if (existing.isPresent()) {
				result.reject("global", "A unit with this name or abbreviation already exists.");
			}
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			redirectAttributes.addFlashAttribute("showManageUnitsModal", true);
			return "redirect:/admin/inventory";
		}

		try {
			UnitOfMeasure newUnit = new UnitOfMeasure(unitDto.getName().trim(), unitDto.getAbbreviation().trim());
			unitOfMeasureService.save(newUnit);
			activityLogService.logAdminAction(principal.getName(), "ADD_UNIT_OF_MEASURE",
					"Added new unit: " + newUnit.getName() + " (" + newUnit.getAbbreviation() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Unit '" + newUnit.getName() + "' added successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error adding unit: " + e.getMessage());
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			redirectAttributes.addFlashAttribute("showManageUnitsModal", true);
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/inventory/units/delete/{id}")
	public String deleteUnitOfMeasure(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		Optional<UnitOfMeasure> unitOpt = unitOfMeasureService.findById(id);
		if (unitOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Unit not found.");
			return "redirect:/admin/inventory";
		}

		UnitOfMeasure unit = unitOpt.get();

		if (unit.getItems() != null && !unit.getItems().isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Cannot delete '" + unit.getName()
					+ "'. It is associated with " + unit.getItems().size() + " inventory item(s).");
			return "redirect:/admin/inventory";
		}

		try {
			String unitName = unit.getName();
			unitOfMeasureService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_UNIT_OF_MEASURE",
					"Deleted unit: " + unitName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess", "Unit '" + unitName + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error deleting unit: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}
	// --- End Inventory Management ---

	// --- Other Mappings ---
	@GetMapping("/transactions")
	public String viewTransactions() {
		return "admin/transactions";
	}

	@GetMapping("/settings")
	public String siteSettings() {
		return "admin/settings";
	}

	@GetMapping("/activity-log")
	public String showActivityLog(Model model) {
		model.addAttribute("activityLogs", activityLogService.getAllLogs());
		return "admin/activity-log";
	}
}