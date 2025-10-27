package com.toastedsiopao.controller;

// --- DTO Imports ---
import com.toastedsiopao.dto.AdminAdminUpdateDto;
import com.toastedsiopao.dto.AdminCustomerUpdateDto;
import com.toastedsiopao.dto.AdminUserCreateDto;
import com.toastedsiopao.dto.CategoryDto; // Product Category
import com.toastedsiopao.dto.InventoryCategoryDto; // Inventory Category
import com.toastedsiopao.dto.InventoryItemDto; // Inventory Item
import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.dto.UnitOfMeasureDto; // Unit of Measure

// --- Model Imports ---
import com.toastedsiopao.model.Category; // Product Category
import com.toastedsiopao.model.InventoryCategory; // Inventory Category
import com.toastedsiopao.model.InventoryItem; // Inventory Item
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.UnitOfMeasure; // Unit of Measure
import com.toastedsiopao.model.User;

// --- Service Imports ---
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.CategoryService; // Product Category
import com.toastedsiopao.service.InventoryCategoryService; // Inventory Category
import com.toastedsiopao.service.InventoryItemService; // Inventory Item
import com.toastedsiopao.service.OrderService;
import com.toastedsiopao.service.ProductService;
import com.toastedsiopao.service.UnitOfMeasureService; // Unit of Measure
import com.toastedsiopao.service.UserService;

// --- Other Imports ---
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

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
	// --- NEW: Inject Inventory Services ---
	@Autowired
	private InventoryItemService inventoryItemService;
	@Autowired
	private InventoryCategoryService inventoryCategoryService;
	@Autowired
	private UnitOfMeasureService unitOfMeasureService;
	// --- End Injection ---

	@GetMapping("/dashboard")
	public String adminDashboard() {
		return "admin/dashboard";
	}

	// --- Product Management ---
	// ... (Product methods remain the same) ...
	@GetMapping("/products")
	public String manageProducts(Model model, @RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "keyword", required = false) String keyword) {

		List<Product> productList;
		List<Category> categoryList = categoryService.findAll();

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
			RedirectAttributes redirectAttributes, Principal principal) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", result);
			redirectAttributes.addFlashAttribute("productDto", productDto);
			redirectAttributes.addFlashAttribute("showAddProductModal", true);
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

		try {
			String productName = productOpt.get().getName();
			productService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_PRODUCT",
					"Deleted product: " + productName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + productName + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("productError", "Error deleting product: " + e.getMessage());
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

		if (!model.containsAttribute("productDto")) {
			ProductDto productDto = new ProductDto();
			productDto.setId(product.getId());
			productDto.setName(product.getName());
			productDto.setDescription(product.getDescription());
			productDto.setPrice(product.getPrice());
			productDto.setCategoryId(product.getCategory().getId());
			productDto.setImageUrl(product.getImageUrl());
			model.addAttribute("productDto", productDto);
		}

		model.addAttribute("categories", categoryService.findAll());
		return "admin/edit-product";
	}

	@PostMapping("/products/update")
	public String updateProduct(@Valid @ModelAttribute("productDto") ProductDto productDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, Model model) {

		if (result.hasErrors()) {
			model.addAttribute("categories", categoryService.findAll());
			return "admin/edit-product";
		}

		try {
			Product savedProduct = productService.save(productDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_PRODUCT",
					"Updated product: " + savedProduct.getName() + " (ID: " + savedProduct.getId() + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + savedProduct.getName() + "' updated successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("productError", "Error updating product: " + e.getMessage());
			redirectAttributes.addFlashAttribute("productDto", productDto);
			return "redirect:/admin/products/edit/" + productDto.getId();
		}

		return "redirect:/admin/products";
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
			return "redirect:/admin/products";
		}

		try {
			Category newCategory = new Category(categoryDto.getName().trim());
			categoryService.save(newCategory);
			activityLogService.logAdminAction(principal.getName(), "ADD_CATEGORY",
					"Added new category: " + newCategory.getName());
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + newCategory.getName() + "' added successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("categoryError", "Error adding category: " + e.getMessage());
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showCategoryModal", true);
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
					"Deleted category: " + categoryName + " (ID: " + id + ")");
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
	// ... (Customer methods remain the same) ...
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
		if (!model.containsAttribute("adminUpdateDto")) {
			model.addAttribute("adminUpdateDto", new AdminAdminUpdateDto());
		}

		return "admin/customers";
	}
	// ... (rest of customer/admin CRUD methods) ...
	// --- End Customer/User Management ---

	// --- NEW: Inventory Management ---
	@GetMapping("/inventory")
	public String manageInventory(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) Long categoryId) {

		// Fetch items based on search/filter
		List<InventoryItem> items = inventoryItemService.searchItems(keyword, categoryId);
		List<InventoryCategory> categories = inventoryCategoryService.findAll();
		List<UnitOfMeasure> units = unitOfMeasureService.findAll();

		model.addAttribute("inventoryItems", items);
		model.addAttribute("inventoryCategories", categories);
		model.addAttribute("unitsOfMeasure", units);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategoryId", categoryId);

		// Add empty DTOs for modal forms
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

	// --- Add/Update Inventory Item ---
	@PostMapping("/inventory/save")
	public String saveInventoryItem(@Valid @ModelAttribute("inventoryItemDto") InventoryItemDto itemDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryItemDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			redirectAttributes.addFlashAttribute("showAddItemModal", true); // Trigger modal re-open
			return "redirect:/admin/inventory";
		}

		try {
			boolean isNew = itemDto.getId() == null;
			InventoryItem savedItem = inventoryItemService.save(itemDto);
			String action = isNew ? "ADD_INVENTORY_ITEM" : "EDIT_INVENTORY_ITEM";
			String message = isNew ? "added" : "updated";
			activityLogService.logAdminAction(principal.getName(), action,
					message.substring(0, 1).toUpperCase() + message.substring(1) + " inventory item: "
							+ savedItem.getName() + " (ID: " + savedItem.getId() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Item '" + savedItem.getName() + "' " + message + " successfully!");
		} catch (IllegalArgumentException e) { // Catch specific errors like duplicate name or bad thresholds
			redirectAttributes.addFlashAttribute("inventoryError", "Error saving item: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto); // Send DTO back
			redirectAttributes.addFlashAttribute("showAddItemModal", true); // Re-open modal
			return "redirect:/admin/inventory";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			redirectAttributes.addFlashAttribute("showAddItemModal", true);
		}

		return "redirect:/admin/inventory";
	}

	// --- Delete Inventory Item ---
	@PostMapping("/inventory/delete/{id}")
	public String deleteInventoryItem(@PathVariable Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<InventoryItem> itemOpt = inventoryItemService.findById(id);
		if (itemOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Item not found. Could not delete.");
			return "redirect:/admin/inventory";
		}
		// TODO: Add check here to prevent deletion if item is used in a Product recipe

		try {
			String itemName = itemOpt.get().getName();
			inventoryItemService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_ITEM",
					"Deleted inventory item: " + itemName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess", "Item '" + itemName + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error deleting item: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}

	// --- Add Inventory Category ---
	@PostMapping("/inventory/categories/add")
	public String addInventoryCategory(@Valid @ModelAttribute("inventoryCategoryDto") InventoryCategoryDto categoryDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (categoryDto.getName() != null && !categoryDto.getName().trim().isEmpty()) {
			if (inventoryCategoryService.findByName(categoryDto.getName().trim()).isPresent()) {
				result.rejectValue("name", "duplicate", "Inventory category name already exists");
			}
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryCategoryDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showManageCategoriesModal", true); // Trigger modal re-open
			return "redirect:/admin/inventory";
		}

		try {
			InventoryCategory newCategory = inventoryCategoryService
					.save(new InventoryCategory(categoryDto.getName().trim()));
			activityLogService.logAdminAction(principal.getName(), "ADD_INVENTORY_CATEGORY",
					"Added inventory category: " + newCategory.getName());
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Category '" + newCategory.getName() + "' added successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error adding category: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showManageCategoriesModal", true);
		}
		return "redirect:/admin/inventory";
	}

	// --- Delete Inventory Category ---
	@PostMapping("/inventory/categories/delete/{id}")
	public String deleteInventoryCategory(@PathVariable Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<InventoryCategory> catOpt = inventoryCategoryService.findById(id);
		if (catOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Category not found.");
			return "redirect:/admin/inventory";
		}
		// TODO: Check if category is in use by any items before deleting

		try {
			String catName = catOpt.get().getName();
			inventoryCategoryService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_CATEGORY",
					"Deleted inventory category: " + catName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Category '" + catName + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error deleting category: " + e.getMessage());
		}
		// Ensure modal re-opens if needed, although usually deletion is final
		// redirectAttributes.addFlashAttribute("showManageCategoriesModal", true);
		return "redirect:/admin/inventory";
	}

	// --- Add Unit of Measure ---
	@PostMapping("/inventory/units/add")
	public String addUnitOfMeasure(@Valid @ModelAttribute("unitOfMeasureDto") UnitOfMeasureDto unitDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (unitDto.getName() != null && !unitDto.getName().trim().isEmpty() && unitDto.getAbbreviation() != null
				&& !unitDto.getAbbreviation().trim().isEmpty()) {
			if (unitOfMeasureService
					.findByNameOrAbbreviation(unitDto.getName().trim(), unitDto.getAbbreviation().trim()).isPresent()) {
				result.rejectValue("name", "duplicate", "Unit name or abbreviation already exists");
			}
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			redirectAttributes.addFlashAttribute("showManageUnitsModal", true); // Trigger modal re-open
			return "redirect:/admin/inventory";
		}

		try {
			UnitOfMeasure newUnit = unitOfMeasureService
					.save(new UnitOfMeasure(unitDto.getName().trim(), unitDto.getAbbreviation().trim()));
			activityLogService.logAdminAction(principal.getName(), "ADD_UNIT_OF_MEASURE",
					"Added unit: " + newUnit.getName() + " (" + newUnit.getAbbreviation() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Unit '" + newUnit.getName() + "' added successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error adding unit: " + e.getMessage());
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			redirectAttributes.addFlashAttribute("showManageUnitsModal", true);
		}
		return "redirect:/admin/inventory";
	}

	// --- Delete Unit of Measure ---
	@PostMapping("/inventory/units/delete/{id}")
	public String deleteUnitOfMeasure(@PathVariable Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<UnitOfMeasure> unitOpt = unitOfMeasureService.findById(id);
		if (unitOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Unit not found.");
			return "redirect:/admin/inventory";
		}
		// TODO: Check if unit is in use by any items before deleting

		try {
			String unitName = unitOpt.get().getName();
			unitOfMeasureService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_UNIT_OF_MEASURE",
					"Deleted unit: " + unitName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess", "Unit '" + unitName + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("inventoryError", "Error deleting unit: " + e.getMessage());
		}
		// Ensure modal re-opens if needed
		// redirectAttributes.addFlashAttribute("showManageUnitsModal", true);
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