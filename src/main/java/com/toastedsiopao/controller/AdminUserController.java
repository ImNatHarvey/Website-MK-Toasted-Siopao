package com.toastedsiopao.controller;

import com.toastedsiopao.dto.AdminAdminUpdateDto;
import com.toastedsiopao.dto.AdminCustomerUpdateDto;
import com.toastedsiopao.dto.AdminUserCreateDto;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/customers")
public class AdminUserController {

	private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

	@Autowired
	private UserService userService;
	@Autowired
	private ActivityLogService activityLogService;

	@GetMapping // Unchanged GET mapping logic
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
		if (!model.containsAttribute("customerUpdateDto")) {
			model.addAttribute("customerUpdateDto", new AdminCustomerUpdateDto());
		}

		return "admin/customers";
	}

	// Helper method to add common attributes for redirect-on-error scenarios
	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers()); // Assuming no search keyword
																							// here
		redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());
		// Add other common lists if needed by the main page or other modals
	}

	@PostMapping("/add-admin")
	public String addAdminUser(@Valid @ModelAttribute("adminUserDto") AdminUserCreateDto userDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		// --- Removed manual username/password checks ---

		// Check only for DTO validation errors first
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUserDto", result);
			redirectAttributes.addFlashAttribute("adminUserDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes); // Add common lists
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "manageAdminsModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles username/password/email validation
			User savedUser = userService.saveAdminUser(userDto, "ROLE_ADMIN");
			activityLogService.logAdminAction(principal.getName(), "ADD_USER (ADMIN)",
					"Created new admin user: " + savedUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Admin user '" + savedUser.getUsername() + "' created successfully!");

		} catch (IllegalArgumentException e) { // Catch validation errors from service
			log.warn("Validation error creating admin user: {}", e.getMessage());
			// Add specific error back to BindingResult or use a generic flash attribute
			if (e.getMessage().contains("Username already exists")) {
				result.rejectValue("username", "userDto.username", e.getMessage());
				// **** ADDED EMAIL HANDLING ****
			} else if (e.getMessage().contains("Email already exists")) {
				result.rejectValue("email", "userDto.email", e.getMessage());
				// **** END ADDED HANDLING ****
			} else if (e.getMessage().contains("Passwords do not match")) {
				result.rejectValue("confirmPassword", "userDto.confirmPassword", e.getMessage());
			} else {
				// Generic error for other validation issues from service
				redirectAttributes.addFlashAttribute("customerError", "Error creating admin: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUserDto", result);
			redirectAttributes.addFlashAttribute("adminUserDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes); // Add common lists
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "manageAdminsModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (Exception e) { // Catch other unexpected errors
			log.error("Unexpected error creating admin user: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("adminUserDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes); // Add common lists
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "manageAdminsModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/customers"; // Success redirect
	}

	@PostMapping("/add-customer")
	public String addCustomerUser(@Valid @ModelAttribute("customerUserDto") AdminUserCreateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		// --- Removed manual username/password checks ---

		// Check only for DTO validation errors first
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUserDto",
					result);
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "addCustomerModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles username/password/email validation
			User savedUser = userService.saveAdminUser(userDto, "ROLE_CUSTOMER"); // Still use saveAdminUser, but pass
																					// correct role
			activityLogService.logAdminAction(principal.getName(), "ADD_USER (CUSTOMER)",
					"Created new customer user: " + savedUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer user '" + savedUser.getUsername() + "' created successfully!");

		} catch (IllegalArgumentException e) { // Catch validation errors from service
			log.warn("Validation error creating customer user: {}", e.getMessage());
			if (e.getMessage().contains("Username already exists")) {
				result.rejectValue("username", "userDto.username", e.getMessage());
				// **** ADDED EMAIL HANDLING ****
			} else if (e.getMessage().contains("Email already exists")) {
				result.rejectValue("email", "userDto.email", e.getMessage());
				// **** END ADDED HANDLING ****
			} else if (e.getMessage().contains("Passwords do not match")) {
				result.rejectValue("confirmPassword", "userDto.confirmPassword", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("customerError", "Error creating customer: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUserDto",
					result);
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "addCustomerModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;

		} catch (Exception e) { // Catch other unexpected errors
			log.error("Unexpected error creating customer user: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "addCustomerModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/customers"; // Success redirect
	}

	@PostMapping("/update") // Update Customer
	public String updateCustomer(@Valid @ModelAttribute("customerUpdateDto") AdminCustomerUpdateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		// --- Removed manual username check ---

		// Check DTO validation errors first
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editCustomerModal")
					.queryParam("editId", userDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles username/email validation on update
			User updatedUser = userService.updateCustomer(userDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_USER (CUSTOMER)",
					"Updated customer user: " + updatedUser.getUsername() + " (ID: " + updatedUser.getId() + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer '" + updatedUser.getUsername() + "' updated successfully!");

		} catch (IllegalArgumentException e) { // Catch validation errors from service
			log.warn("Validation error updating customer: {}", e.getMessage());
			if (e.getMessage().contains("Username already exists") || e.getMessage().contains("Username '")) {
				result.rejectValue("username", "userDto.username", e.getMessage());
				// **** ADDED EMAIL HANDLING ****
			} else if (e.getMessage().contains("Email already exists") || e.getMessage().contains("Email '")) {
				result.rejectValue("email", "userDto.email", e.getMessage());
				// **** END ADDED HANDLING ****
			} else {
				redirectAttributes.addFlashAttribute("customerError", "Error updating customer: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editCustomerModal")
					.queryParam("editId", userDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (Exception e) { // Catch other unexpected errors
			log.error("Error updating customer: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editCustomerModal")
					.queryParam("editId", userDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/customers"; // Success redirect
	}

	@PostMapping("/admin/update") // Update Admin
	public String updateAdmin(@Valid @ModelAttribute("adminUpdateDto") AdminAdminUpdateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		// --- Removed manual username check ---

		// Check DTO validation errors first
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUpdateDto", result);
			redirectAttributes.addFlashAttribute("adminUpdateDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editAdminModal")
					.queryParam("editId", userDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles username/email validation on update
			User updatedUser = userService.updateAdmin(userDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_USER (ADMIN)",
					"Updated admin user: " + updatedUser.getUsername() + " (ID: " + updatedUser.getId() + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Admin '" + updatedUser.getUsername() + "' updated successfully!");

		} catch (IllegalArgumentException e) { // Catch validation errors from service
			log.warn("Validation error updating admin: {}", e.getMessage());
			if (e.getMessage().contains("Username already exists") || e.getMessage().contains("Username '")) {
				result.rejectValue("username", "userDto.username", e.getMessage());
				// **** ADDED EMAIL HANDLING ****
			} else if (e.getMessage().contains("Email already exists") || e.getMessage().contains("Email '")) {
				result.rejectValue("email", "userDto.email", e.getMessage());
				// **** END ADDED HANDLING ****
			} else {
				redirectAttributes.addFlashAttribute("customerError", "Error updating admin: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUpdateDto", result);
			redirectAttributes.addFlashAttribute("adminUpdateDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editAdminModal")
					.queryParam("editId", userDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (Exception e) { // Catch other unexpected errors
			log.error("Error updating admin: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("adminUpdateDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editAdminModal")
					.queryParam("editId", userDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/customers"; // Success redirect
	}

	// --- Delete methods remain unchanged ---
	@PostMapping("/delete/{id}") // Deletes only customers
	public String deleteCustomer(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<User> userOpt = userService.findUserById(id);
		// Basic check if user exists and is a customer
		if (userOpt.isEmpty() || !"ROLE_CUSTOMER".equals(userOpt.get().getRole())) {
			redirectAttributes.addFlashAttribute("customerError", "Customer not found or invalid ID.");
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
			log.error("Error deleting customer ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "Error deleting customer: " + e.getMessage());
		}
		return "redirect:/admin/customers";
	}

	@PostMapping("/admin/delete/{id}") // Separate path for deleting admins
	public String deleteAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Principal principal) {
		Optional<User> userOpt = userService.findUserById(id);
		// Basic check if user exists and is an admin
		if (userOpt.isEmpty() || !"ROLE_ADMIN".equals(userOpt.get().getRole())) {
			redirectAttributes.addFlashAttribute("customerError", "Admin not found or invalid ID.");
			return "redirect:/admin/customers";
		}
		String username = userOpt.get().getUsername();
		// Prevent self-deletion
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
			log.error("Error deleting admin ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "Error deleting admin: " + e.getMessage());
		}
		return "redirect:/admin/customers";
	}
}