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
import org.springframework.web.util.UriComponentsBuilder; // **** IMPORTED ****

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

	@GetMapping
	public String manageCustomers(Model model, Principal principal,
			@RequestParam(value = "keyword", required = false) String keyword) {
		// ... (fetching users - unchanged) ...
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

		// Add DTOs if not present
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

	@PostMapping("/add-admin")
	public String addAdminUser(@Valid @ModelAttribute("adminUserDto") AdminUserCreateDto userDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) { // **** ADDED
																											// UriComponentsBuilder
																											// ****

		// ... (validation checks - unchanged) ...
		if (userService.findByUsername(userDto.getUsername()) != null) {
			result.rejectValue("username", "userDto.username", "Username already exists");
		}
		if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "userDto.confirmPassword", "Passwords do not match");
		}

		if (result.hasErrors()) {
			// **** MODIFIED: Add URL parameter ****
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUserDto", result);
			redirectAttributes.addFlashAttribute("adminUserDto", userDto);
			// Pass necessary lists for main page
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers()); // Assuming no search
																								// keyword here
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());

			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "manageAdminsModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		// ... (success/error handling - unchanged, but add redirect param on error) ...
		try {
			User savedUser = userService.saveAdminUser(userDto, "ROLE_ADMIN");
			activityLogService.logAdminAction(principal.getName(), "ADD_USER (ADMIN)",
					/* ... */ "Created new admin user: " + savedUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					/* ... */ "Admin user '" + savedUser.getUsername() + "' created successfully!");
		} catch (Exception e) {
			log.error("Error creating admin user: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "Error creating admin: " + e.getMessage());
			redirectAttributes.addFlashAttribute("adminUserDto", userDto);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers());
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());
			// **** MODIFIED: Redirect with parameter ****
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "manageAdminsModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/customers"; // Success redirect
	}

	@PostMapping("/add-customer")
	public String addCustomerUser(@Valid @ModelAttribute("customerUserDto") AdminUserCreateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) { // **** ADDED UriComponentsBuilder ****

		// ... (validation checks - unchanged) ...
		if (userService.findByUsername(userDto.getUsername()) != null) {
			result.rejectValue("username", "userDto.username", "Username already exists");
		}
		if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "userDto.confirmPassword", "Passwords do not match");
		}

		if (result.hasErrors()) {
			// **** MODIFIED: Add URL parameter ****
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUserDto",
					result);
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers());
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());

			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "addCustomerModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		// ... (success/error handling - unchanged, but add redirect param on error) ...
		try {
			User savedUser = userService.saveAdminUser(userDto, "ROLE_CUSTOMER");
			activityLogService.logAdminAction(principal.getName(), "ADD_USER (CUSTOMER)",
					/* ... */ "Created new customer user: " + savedUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					/* ... */ "Customer user '" + savedUser.getUsername() + "' created successfully!");
		} catch (Exception e) {
			log.error("Error creating customer user: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "Error creating customer: " + e.getMessage());
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers());
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());
			// **** MODIFIED: Redirect with parameter ****
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "addCustomerModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/customers"; // Success redirect
	}

	@PostMapping("/update") // Update Customer
	public String updateCustomer(@Valid @ModelAttribute("customerUpdateDto") AdminCustomerUpdateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) { // **** ADDED UriComponentsBuilder ****

		if (result.hasErrors()) {
			// **** MODIFIED: Add URL parameter ****
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers());
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());

			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editCustomerModal")
					.queryParam("editId", userDto.getId()) // Optional
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		// ... (success/error handling - unchanged, but add redirect param on error) ...
		try {
			User updatedUser = userService.updateCustomer(userDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_USER (CUSTOMER)",
					/* ... */ "Updated customer user: " + updatedUser.getUsername() + " (ID: " + updatedUser.getId()
							+ ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					/* ... */ "Customer '" + updatedUser.getUsername() + "' updated successfully!");
		} catch (IllegalArgumentException e) {
			log.warn("Validation error updating customer: {}", e.getMessage());
			result.rejectValue("username", "userDto.username", e.getMessage()); // Assuming username is the likely cause
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers());
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());
			// **** MODIFIED: Redirect with parameter ****
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editCustomerModal")
					.queryParam("editId", userDto.getId()) // Optional
					.build().toUriString();
			return "redirect:" + redirectUrl;
		} catch (Exception e) {
			log.error("Error updating customer: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "Error updating customer: " + e.getMessage());
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers());
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());
			// **** MODIFIED: Redirect with parameter ****
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editCustomerModal")
					.queryParam("editId", userDto.getId()) // Optional
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/customers"; // Success redirect
	}

	@PostMapping("/admin/update") // Update Admin
	public String updateAdmin(@Valid @ModelAttribute("adminUpdateDto") AdminAdminUpdateDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) { // **** ADDED UriComponentsBuilder ****

		if (result.hasErrors()) {
			// **** MODIFIED: Add URL parameter ****
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUpdateDto", result);
			redirectAttributes.addFlashAttribute("adminUpdateDto", userDto);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers());
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());

			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editAdminModal")
					.queryParam("editId", userDto.getId()) // Optional
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		// ... (success/error handling - unchanged, but add redirect param on error) ...
		try {
			User updatedUser = userService.updateAdmin(userDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_USER (ADMIN)", /* ... */ "Updated admin user: "
					+ updatedUser.getUsername() + " (ID: " + updatedUser.getId() + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					/* ... */ "Admin '" + updatedUser.getUsername() + "' updated successfully!");
		} catch (IllegalArgumentException e) {
			log.warn("Validation error updating admin: {}", e.getMessage());
			result.rejectValue("username", "userDto.username", e.getMessage());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUpdateDto", result);
			redirectAttributes.addFlashAttribute("adminUpdateDto", userDto);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers());
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());
			// **** MODIFIED: Redirect with parameter ****
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editAdminModal")
					.queryParam("editId", userDto.getId()) // Optional
					.build().toUriString();
			return "redirect:" + redirectUrl;
		} catch (Exception e) {
			log.error("Error updating admin: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "Error updating admin: " + e.getMessage());
			redirectAttributes.addFlashAttribute("adminUpdateDto", userDto);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("customers", userService.findAllCustomers());
			redirectAttributes.addFlashAttribute("admins", userService.findAllAdmins());
			// **** MODIFIED: Redirect with parameter ****
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editAdminModal")
					.queryParam("editId", userDto.getId()) // Optional
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/customers"; // Success redirect
	}

	// --- UNCHANGED Methods ---
	@PostMapping("/delete/{id}") // Deletes only customers
	public String deleteCustomer(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		// ... (logic unchanged) ...
		Optional<User> userOpt = userService.findUserById(id);
		if (userOpt.isEmpty() || !userOpt.get().getRole().equals("ROLE_CUSTOMER")) {
			/* ... */ return "redirect:/admin/customers";
		}
		String username = userOpt.get().getUsername();
		try {
			userService.deleteUserById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_USER (CUSTOMER)",
					/* ... */ "Deleted customer user: " + username + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					/* ... */ "Customer '" + username + "' deleted successfully!");
		} catch (Exception e) {
			/* ... */ redirectAttributes.addFlashAttribute("customerError",
					"Error deleting customer: " + e.getMessage());
		}
		return "redirect:/admin/customers";
	}

	@PostMapping("/admin/delete/{id}") // Separate path for deleting admins
	public String deleteAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Principal principal) {
		// ... (logic unchanged) ...
		Optional<User> userOpt = userService.findUserById(id);
		if (userOpt.isEmpty() || !userOpt.get().getRole().equals("ROLE_ADMIN")) {
			/* ... */ return "redirect:/admin/customers";
		}
		String username = userOpt.get().getUsername();
		if (principal.getName().equals(username)) {
			/* ... */ redirectAttributes.addFlashAttribute("customerError", "You cannot delete your own account.");
			return "redirect:/admin/customers";
		}
		try {
			userService.deleteUserById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_USER (ADMIN)",
					/* ... */ "Deleted admin user: " + username + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					/* ... */ "Admin '" + username + "' deleted successfully!");
		} catch (Exception e) {
			/* ... */ redirectAttributes.addFlashAttribute("customerError", "Error deleting admin: " + e.getMessage());
		}
		return "redirect:/admin/customers";
	}
}