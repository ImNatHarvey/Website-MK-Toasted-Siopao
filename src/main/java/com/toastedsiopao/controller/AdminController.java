package com.toastedsiopao.controller;

import com.toastedsiopao.dto.AdminAdminUpdateDto;
import com.toastedsiopao.dto.AdminCustomerUpdateDto;
import com.toastedsiopao.dto.AdminUserCreateDto;
import com.toastedsiopao.dto.CategoryDto;
import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.CategoryService;
import com.toastedsiopao.service.ProductService;
import com.toastedsiopao.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils; // Import StringUtils

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
	private CategoryService categoryService;

	@Autowired
	private ActivityLogService activityLogService;

	@Autowired
	private UserService userService;
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

	@GetMapping("/orders")
	public String manageOrders() {
		return "admin/orders";
	}

	// --- Customer/User Management ---

	// UPDATED: Added @RequestParam for keyword
	@GetMapping("/customers")
	public String manageCustomers(Model model, Principal principal,
			@RequestParam(value = "keyword", required = false) String keyword) {

		// NEW: Fetch customers based on keyword
		List<User> customers;
		if (StringUtils.hasText(keyword)) {
			customers = userService.searchCustomers(keyword);
			model.addAttribute("keyword", keyword); // Send keyword back to view
		} else {
			customers = userService.findAllCustomers();
		}
		model.addAttribute("customers", customers);
		// END NEW

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

	@PostMapping("/customers/add-admin")
	public String addAdmin(@Valid @ModelAttribute("adminUserDto") AdminUserCreateDto userDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal) {

		if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
				&& !userDto.getPassword().equals(userDto.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "passwords.mismatch", "Passwords do not match");
		}

		if (userService.findByUsername(userDto.getUsername()) != null) {
			result.rejectValue("username", "username.exists", "Username already exists");
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUserDto", result);
			redirectAttributes.addFlashAttribute("adminUserDto", userDto);
			redirectAttributes.addFlashAttribute("showManageAdminsModal", true);
			return "redirect:/admin/customers";
		}

		try {
			User newUser = userService.saveAdminUser(userDto, "ROLE_ADMIN");
			activityLogService.logAdminAction(principal.getName(), "CREATE_ADMIN",
					"Created new admin user: " + newUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Admin user '" + newUser.getUsername() + "' created successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error creating admin user: " + e.getMessage());
			redirectAttributes.addFlashAttribute("adminUserDto", userDto);
			redirectAttributes.addFlashAttribute("showManageAdminsModal", true);
		}

		return "redirect:/admin/customers";
	}

	@PostMapping("/customers/add-customer")
	public String addCustomer(@Valid @ModelAttribute("customerUserDto") AdminUserCreateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
				&& !userDto.getPassword().equals(userDto.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "passwords.mismatch", "Passwords do not match");
		}

		if (userService.findByUsername(userDto.getUsername()) != null) {
			result.rejectValue("username", "username.exists", "Username already exists");
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUserDto",
					result);
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			redirectAttributes.addFlashAttribute("showAddCustomerModal", true);
			return "redirect:/admin/customers";
		}

		try {
			User newUser = userService.saveAdminUser(userDto, "ROLE_CUSTOMER");
			activityLogService.logAdminAction(principal.getName(), "CREATE_CUSTOMER",
					"Created new customer user: " + newUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer user '" + newUser.getUsername() + "' created successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error creating customer user: " + e.getMessage());
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			redirectAttributes.addFlashAttribute("showAddCustomerModal", true);
		}

		return "redirect:/admin/customers";
	}

	@GetMapping("/customers/edit/{id}")
	public String showEditCustomerForm(@PathVariable("id") Long id, Model model,
			RedirectAttributes redirectAttributes) {

		Optional<User> userOpt = userService.findUserById(id);

		if (userOpt.isEmpty() || !"ROLE_CUSTOMER".equals(userOpt.get().getRole())) {
			redirectAttributes.addFlashAttribute("customerError", "Customer not found (ID: " + id + "). Cannot edit.");
			return "redirect:/admin/customers";
		}

		User user = userOpt.get();

		if (!model.containsAttribute("customerUpdateDto")) {
			AdminCustomerUpdateDto dto = new AdminCustomerUpdateDto();
			dto.setId(user.getId());
			dto.setFirstName(user.getFirstName());
			dto.setLastName(user.getLastName());
			dto.setUsername(user.getUsername());
			dto.setPhone(user.getPhone());
			dto.setHouseNo(user.getHouseNo());
			dto.setLotNo(user.getLotNo());
			dto.setBlockNo(user.getBlockNo());
			dto.setStreet(user.getStreet());
			dto.setBarangay(user.getBarangay());
			dto.setMunicipality(user.getMunicipality());
			dto.setProvince(user.getProvince());
			model.addAttribute("customerUpdateDto", dto);
		}

		return "admin/edit-customer";
	}

	@PostMapping("/customers/update")
	public String updateCustomer(@Valid @ModelAttribute("customerUpdateDto") AdminCustomerUpdateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal, Model model) {

		if (result.hasErrors()) {
			return "admin/edit-customer";
		}

		try {
			User updatedUser = userService.updateCustomer(userDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_CUSTOMER",
					"Updated customer user: " + updatedUser.getUsername() + " (ID: " + updatedUser.getId() + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer '" + updatedUser.getUsername() + "' updated successfully!");
		} catch (IllegalArgumentException e) {
			result.rejectValue("username", "username.exists", e.getMessage());
			model.addAttribute("customerUpdateDto", userDto);
			return "admin/edit-customer";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error updating customer: " + e.getMessage());
			return "redirect:/admin/customers/edit/" + userDto.getId();
		}

		return "redirect:/admin/customers";
	}

	@PostMapping("/customers/delete/{id}")
	public String deleteCustomer(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		Optional<User> userOpt = userService.findUserById(id);
		if (userOpt.isEmpty() || !"ROLE_CUSTOMER".equals(userOpt.get().getRole())) {
			redirectAttributes.addFlashAttribute("customerError", "Customer not found. Could not delete.");
			return "redirect:/admin/customers";
		}

		try {
			String username = userOpt.get().getUsername();
			userService.deleteUserById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_CUSTOMER",
					"Deleted customer: " + username + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer '" + username + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error deleting customer: " + e.getMessage());
		}

		return "redirect:/admin/customers";
	}

	@GetMapping("/customers/admin/edit/{id}")
	public String showEditAdminForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {

		Optional<User> userOpt = userService.findUserById(id);

		if (userOpt.isEmpty() || !"ROLE_ADMIN".equals(userOpt.get().getRole())) {
			redirectAttributes.addFlashAttribute("customerError",
					"Admin user not found (ID: " + id + "). Cannot edit.");
			return "redirect:/admin/customers";
		}

		User user = userOpt.get();

		if (!model.containsAttribute("adminUpdateDto")) {
			AdminAdminUpdateDto dto = new AdminAdminUpdateDto();
			dto.setId(user.getId());
			dto.setFirstName(user.getFirstName());
			dto.setLastName(user.getLastName());
			dto.setUsername(user.getUsername());
			model.addAttribute("adminUpdateDto", dto);
		}

		return "admin/edit-admin";
	}

	@PostMapping("/customers/admin/update")
	public String updateAdmin(@Valid @ModelAttribute("adminUpdateDto") AdminAdminUpdateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal, Model model) {

		if (result.hasErrors()) {
			model.addAttribute("adminUpdateDto", userDto);
			return "admin/edit-admin";
		}

		try {
			User updatedUser = userService.updateAdmin(userDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_ADMIN",
					"Updated admin user: " + updatedUser.getUsername() + " (ID: " + updatedUser.getId() + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Admin '" + updatedUser.getUsername() + "' updated successfully!");
		} catch (IllegalArgumentException e) {
			result.rejectValue("username", "username.exists", e.getMessage());
			model.addAttribute("adminUpdateDto", userDto);
			return "admin/edit-admin";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("adminError", "Error updating admin: " + e.getMessage());
			return "redirect:/admin/customers/admin/edit/" + userDto.getId();
		}

		return "redirect:/admin/customers";
	}

	@PostMapping("/customers/admin/delete/{id}")
	public String deleteAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Principal principal) {

		Optional<User> userOpt = userService.findUserById(id);
		if (userOpt.isEmpty() || !"ROLE_ADMIN".equals(userOpt.get().getRole())) {
			redirectAttributes.addFlashAttribute("customerError", "Admin user not found. Could not delete.");
			return "redirect:/admin/customers";
		}

		User userToDelete = userOpt.get();

		if (userToDelete.getUsername().equals(principal.getName())) {
			redirectAttributes.addFlashAttribute("customerError", "You cannot delete your own account.");
			return "redirect:/admin/customers";
		}

		try {
			String username = userToDelete.getUsername();
			userService.deleteUserById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_ADMIN",
					"Deleted admin user: " + username + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Admin user '" + username + "' deleted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("customerError", "Error deleting admin user: " + e.getMessage());
		}

		return "redirect:/admin/customers";
	}

	// --- Other Mappings ---
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

	@GetMapping("/activity-log")
	public String showActivityLog(Model model) {
		model.addAttribute("activityLogs", activityLogService.getAllLogs());
		return "admin/activity-log";
	}
}