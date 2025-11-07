package com.toastedsiopao.controller;

import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.dto.CustomerUpdateDto;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.RoleRepository; // NEW IMPORT
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.AdminService; // NEW IMPORT
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

	private static final Logger log = LoggerFactory.getLogger(AdminCustomerController.class);

	@Autowired
	private CustomerService customerService; // UPDATED

	@Autowired
	private AdminService adminService; // NEW (for creating accounts)

	@Autowired
	private ActivityLogService activityLogService;

	@Autowired
	private RoleRepository roleRepository; // NEW INJECTION FOR FIX

	@GetMapping
	public String manageCustomers(Model model, Principal principal,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<User> customerPage;

		if (StringUtils.hasText(keyword)) {
			customerPage = customerService.searchCustomers(keyword, pageable); // UPDATED
			model.addAttribute("keyword", keyword);
		} else {
			customerPage = customerService.findAllCustomers(pageable); // UPDATED
		}

		model.addAttribute("customerPage", customerPage);
		model.addAttribute("customers", customerPage.getContent());
		model.addAttribute("currentUsername", principal.getName());
		model.addAttribute("activeCustomerCount", customerService.countActiveCustomers()); // UPDATED

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", customerPage.getTotalPages());
		model.addAttribute("totalItems", customerPage.getTotalElements());
		model.addAttribute("size", size);

		if (!model.containsAttribute("customerUserDto")) {
			// This DTO is used by the "Add Customer" form
			model.addAttribute("customerUserDto", new AdminAccountCreateDto()); // UPDATED DTO
		}
		if (!model.containsAttribute("customerUpdateDto")) {
			model.addAttribute("customerUpdateDto", new CustomerUpdateDto()); // UPDATED DTO
		}

		return "admin/customers";
	}

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		Pageable defaultPageable = PageRequest.of(0, 10);
		redirectAttributes.addFlashAttribute("customers",
				customerService.findAllCustomers(defaultPageable).getContent()); // UPDATED
	}

	@PostMapping("/add-customer")
	public String addCustomerUser(@Valid @ModelAttribute("customerUserDto") AdminAccountCreateDto userDto, // UPDATED
																											// DTO
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

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
			// --- THIS IS THE FIX ---
			// Use the CustomerService to create the customer account
			User savedUser = customerService.createCustomerFromAdmin(userDto);
			// --- END FIX ---

			activityLogService.logAdminAction(principal.getName(), "ADD_USER (CUSTOMER)",
					"Created new customer user: " + savedUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer user '" + savedUser.getUsername() + "' created successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error creating customer user: {}", e.getMessage());

			// --- FIX: Use correct BindingResult object name ---
			if (e.getMessage().contains("Username already exists")) {
				result.rejectValue("username", "customerUserDto.username", e.getMessage());
			} else if (e.getMessage().contains("Email already exists")) {
				result.rejectValue("email", "customerUserDto.email", e.getMessage());
			} else if (e.getMessage().contains("Passwords do not match")) {
				result.rejectValue("confirmPassword", "customerUserDto.confirmPassword", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("customerError", "Error creating customer: " + e.getMessage());
			}
			// --- END FIX ---

			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerUserDto",
					result);
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "addCustomerModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;

		} catch (Exception e) {
			log.error("Unexpected error creating customer user: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("customerUserDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "addCustomerModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/customers";
	}

	@PostMapping("/update") // Update Customer
	public String updateCustomer(@Valid @ModelAttribute("customerUpdateDto") CustomerUpdateDto userDto, // UPDATED DTO
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

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
			User updatedUser = customerService.updateCustomer(userDto); // UPDATED SERVICE CALL
			activityLogService.logAdminAction(principal.getName(), "EDIT_USER (CUSTOMER)",
					"Updated customer user: " + updatedUser.getUsername() + " (ID: " + updatedUser.getId() + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer '" + updatedUser.getUsername() + "' updated successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error updating customer: {}", e.getMessage());
			if (e.getMessage().contains("Username already exists") || e.getMessage().contains("Username '")) {
				result.rejectValue("username", "customerUpdateDto.username", e.getMessage());
			} else if (e.getMessage().contains("Email already exists") || e.getMessage().contains("Email '")) {
				result.rejectValue("email", "customerUpdateDto.email", e.getMessage());
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

		} catch (Exception e) {
			log.error("Error updating customer: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("customerUpdateDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "editCustomerModal")
					.queryParam("editId", userDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/customers";
	}

	// **** METHOD UPDATED ****
	@PostMapping("/delete/{id}")
	public String deleteCustomer(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<User> userOpt = customerService.findUserById(id); // UPDATED
		if (userOpt.isEmpty() || userOpt.get().getRole() == null
				|| !"ROLE_CUSTOMER".equals(userOpt.get().getRole().getName())) { // UPDATED
			redirectAttributes.addFlashAttribute("customerError", "Customer not found or invalid ID.");
			return "redirect:/admin/customers";
		}
		String username = userOpt.get().getUsername();
		try {
			// Service layer now handles validation (e.g., if customer has orders)
			customerService.deleteCustomerById(id); // UPDATED
			activityLogService.logAdminAction(principal.getName(), "DELETE_USER (CUSTOMER)",
					"Deleted customer user: " + username + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer '" + username + "' deleted successfully!");
		} catch (RuntimeException e) {
			// Catch exceptions thrown from the service layer
			log.error("Error deleting customer ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("customerError", "Error deleting customer: " + e.getMessage());
		}
		return "redirect:/admin/customers";
	}
	// **** END OF UPDATED METHOD ****
}