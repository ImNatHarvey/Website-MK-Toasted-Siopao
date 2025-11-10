package com.toastedsiopao.controller;

import com.toastedsiopao.dto.CustomerCreateDto;
import com.toastedsiopao.dto.CustomerUpdateDto;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.RoleRepository;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.AdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
	private CustomerService customerService;

	@Autowired
	private AdminService adminService;

	@Autowired
	private ActivityLogService activityLogService;

	@Autowired
	private RoleRepository roleRepository;

	@GetMapping
	@PreAuthorize("hasAuthority('VIEW_CUSTOMERS')")
	public String manageCustomers(Model model, Principal principal,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<User> customerPage;

		if (StringUtils.hasText(keyword)) {
			customerPage = customerService.searchCustomers(keyword, pageable);
			model.addAttribute("keyword", keyword);
		} else {
			customerPage = customerService.findAllCustomers(pageable);
		}

		model.addAttribute("customerPage", customerPage);
		model.addAttribute("customers", customerPage.getContent());
		model.addAttribute("currentUsername", principal.getName());
		model.addAttribute("activeCustomerCount", customerService.countActiveCustomers());
		model.addAttribute("inactiveCustomerCount", customerService.countInactiveCustomers()); // == ADDED ==

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", customerPage.getTotalPages());
		model.addAttribute("totalItems", customerPage.getTotalElements());
		model.addAttribute("size", size);

		if (!model.containsAttribute("customerCreateDto")) {
			model.addAttribute("customerCreateDto", new CustomerCreateDto());
		}
		if (!model.containsAttribute("customerUpdateDto")) {
			model.addAttribute("customerUpdateDto", new CustomerUpdateDto());
		}

		return "admin/customers";
	}

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		Pageable defaultPageable = PageRequest.of(0, 10);
		redirectAttributes.addFlashAttribute("customers",
				customerService.findAllCustomers(defaultPageable).getContent());
	}

	@PostMapping("/add-customer")
	@PreAuthorize("hasAuthority('ADD_CUSTOMERS')")
	public String addCustomerUser(@Valid @ModelAttribute("customerCreateDto") CustomerCreateDto userDto,

			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerCreateDto",
					result);
			redirectAttributes.addFlashAttribute("customerCreateDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "addCustomerModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			User savedUser = customerService.createCustomerFromAdmin(userDto);

			activityLogService.logAdminAction(principal.getName(), "ADD_USER (CUSTOMER)",
					"Created new customer user: " + savedUser.getUsername());
			redirectAttributes.addFlashAttribute("customerSuccess",
					"Customer user '" + savedUser.getUsername() + "' created successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error creating customer user: {}", e.getMessage());

			if (e.getMessage().contains("Username already exists")) {
				result.rejectValue("username", "customerCreateDto.username", e.getMessage());
			} else if (e.getMessage().contains("Email already exists")) {
				result.rejectValue("email", "customerCreateDto.email", e.getMessage());
			} else if (e.getMessage().contains("Passwords do not match")) {
				result.rejectValue("confirmPassword", "customerCreateDto.confirmPassword", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("customerError", "Error creating customer: " + e.getMessage());
			}

			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerCreateDto",
					result);
			redirectAttributes.addFlashAttribute("customerCreateDto", userDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/customers").queryParam("showModal", "addCustomerModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;

		}
		// --- REMOVED: generic catch (Exception e) block ---

		return "redirect:/admin/customers";
	}

	@PostMapping("/update")
	@PreAuthorize("hasAuthority('EDIT_CUSTOMERS')")
	public String updateCustomer(@Valid @ModelAttribute("customerUpdateDto") CustomerUpdateDto userDto,
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
			User updatedUser = customerService.updateCustomer(userDto);
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

		}
		// --- REMOVED: generic catch (Exception e) block ---

		return "redirect:/admin/customers";
	}

	@PostMapping("/delete/{id}")
	@PreAuthorize("hasAuthority('DELETE_CUSTOMERS')")
	public String deleteCustomer(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		// --- REMOVED: try-catch block ---

		Optional<User> userOpt = customerService.findUserById(id);
		if (userOpt.isEmpty() || userOpt.get().getRole() == null
				|| !"ROLE_CUSTOMER".equals(userOpt.get().getRole().getName())) {
			redirectAttributes.addFlashAttribute("customerError", "Customer not found or invalid ID.");
			return "redirect:/admin/customers";
		}
		String username = userOpt.get().getUsername();

		// Let the service throw an exception (e.g., if customer has orders)
		// The GlobalExceptionHandler will catch it.
		customerService.deleteCustomerById(id);

		activityLogService.logAdminAction(principal.getName(), "DELETE_USER (CUSTOMER)",
				"Deleted customer user: " + username + " (ID: " + id + ")");
		redirectAttributes.addFlashAttribute("customerSuccess", "Customer '" + username + "' deleted successfully!");

		return "redirect:/admin/customers";
	}
}