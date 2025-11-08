package com.toastedsiopao.controller;

import com.toastedsiopao.dto.AdminProfileUpdateDto;
import com.toastedsiopao.dto.AdminUpdateDto;
import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.model.Role;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.AdminService;
import com.toastedsiopao.service.CustomerService;
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
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/admins")
public class AdminManagementController {

	private static final Logger log = LoggerFactory.getLogger(AdminManagementController.class);

	@Autowired
	private AdminService adminService;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private ActivityLogService activityLogService;

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		Pageable defaultPageable = PageRequest.of(0, 10);
		redirectAttributes.addFlashAttribute("admins", adminService.findAllAdmins(defaultPageable).getContent());
	}

	@GetMapping
	@PreAuthorize("hasAuthority('VIEW_ADMINS')")
	public String manageAdmins(Model model, Principal principal,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<User> adminPage = adminService.searchAdmins(keyword, pageable);

		model.addAttribute("adminPage", adminPage);
		model.addAttribute("admins", adminPage.getContent());
		model.addAttribute("currentUsername", principal.getName());
		model.addAttribute("totalItems", adminPage.getTotalElements());
		model.addAttribute("activeAdminCount", adminService.countActiveAdmins());

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", adminPage.getTotalPages());
		model.addAttribute("totalItems", adminPage.getTotalElements());
		model.addAttribute("size", size);
		model.addAttribute("keyword", keyword);

		if (!model.containsAttribute("adminAccountCreateDto")) {
			model.addAttribute("adminAccountCreateDto", new AdminAccountCreateDto());
		}

		if (!model.containsAttribute("adminUpdateDto")) {
			model.addAttribute("adminUpdateDto", new AdminUpdateDto());
		}

		if (!model.containsAttribute("adminProfileDto")) {
			User currentUser = customerService.findByUsername(principal.getName());
			AdminProfileUpdateDto profileDto = new AdminProfileUpdateDto();
			if (currentUser != null) {
				profileDto.setId(currentUser.getId());
				profileDto.setFirstName(currentUser.getFirstName());
				profileDto.setLastName(currentUser.getLastName());
				profileDto.setUsername(currentUser.getUsername());
				profileDto.setEmail(currentUser.getEmail());
			}
			model.addAttribute("adminProfileDto", profileDto);
		}

		return "admin/admins";
	}

	@PostMapping("/add")
	@PreAuthorize("hasRole('OWNER')")
	public String addAdmin(@Valid @ModelAttribute("adminAccountCreateDto") AdminAccountCreateDto adminDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminAccountCreateDto",
					result);
			redirectAttributes.addFlashAttribute("adminAccountCreateDto", adminDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "addAdminModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			User savedUser = adminService.createAccount(adminDto); // UPDATED (role is in DTO)
			activityLogService.logAdminAction(principal.getName(), "ADD_ADMIN", "Created new admin user: "
					+ savedUser.getUsername() + " with role " + savedUser.getRole().getName());
			redirectAttributes.addFlashAttribute("adminSuccess",
					"Admin user '" + savedUser.getUsername() + "' created successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error creating admin user: {}", e.getMessage());
			if (e.getMessage().contains("Username already exists")) {
				result.rejectValue("username", "adminAccountCreateDto.username", e.getMessage());
			} else if (e.getMessage().contains("Email already exists")) {
				result.rejectValue("email", "adminAccountCreateDto.email", e.getMessage());
			} else if (e.getMessage().contains("Passwords do not match")) {
				result.rejectValue("confirmPassword", "adminAccountCreateDto.confirmPassword", e.getMessage());
			} else if (e.getMessage().contains("Role name")) { // NEW
				result.rejectValue("roleName", "adminAccountCreateDto.roleName", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("adminError", "Error creating admin: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminAccountCreateDto",
					result);
			redirectAttributes.addFlashAttribute("adminAccountCreateDto", adminDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "addAdminModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/admins";
	}

	@PostMapping("/update")
	@PreAuthorize("hasRole('OWNER')")
	public String updateAdmin(@Valid @ModelAttribute("adminUpdateDto") AdminUpdateDto adminDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUpdateDto", result);
			redirectAttributes.addFlashAttribute("adminUpdateDto", adminDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "editAdminModal")
					.queryParam("editId", adminDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			User updatedUser = adminService.updateAdmin(adminDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_ADMIN",
					"Updated admin user: " + updatedUser.getUsername() + " (ID: " + updatedUser.getId() + ")");
			redirectAttributes.addFlashAttribute("adminSuccess",
					"Admin '" + updatedUser.getUsername() + "' updated successfully!");

		} catch (RuntimeException e) {
			log.warn("Validation error updating admin: {}", e.getMessage());
			if (e.getMessage().contains("Username already exists") || e.getMessage().contains("Username '")) {
				result.rejectValue("username", "adminUpdateDto.username", e.getMessage());
			} else if (e.getMessage().contains("Email already exists") || e.getMessage().contains("Email '")) {
				result.rejectValue("email", "adminUpdateDto.email", e.getMessage());
			} else if (e.getMessage().contains("Role name")) {
				result.rejectValue("roleName", "adminUpdateDto.roleName", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("adminError", "Error updating admin: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminUpdateDto", result);
			redirectAttributes.addFlashAttribute("adminUpdateDto", adminDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "editAdminModal")
					.queryParam("editId", adminDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/admins";
	}

	@PostMapping("/profile/update")
	@PreAuthorize("isAuthenticated()")
	public String updateAdminProfile(@Valid @ModelAttribute("adminProfileDto") AdminProfileUpdateDto adminDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminProfileDto",
					result);
			redirectAttributes.addFlashAttribute("adminProfileDto", adminDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "editProfileModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		User currentUser = customerService.findByUsername(principal.getName());
		if (currentUser == null || !currentUser.getId().equals(adminDto.getId())) {
			redirectAttributes.addFlashAttribute("adminError", "Security error: Profile mismatch.");
			return "redirect:/admin/admins";
		}

		try {
			User updatedUser = adminService.updateAdminProfile(adminDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_PROFILE",
					"Updated own profile: " + updatedUser.getUsername());
			redirectAttributes.addFlashAttribute("adminSuccess", "Your profile has been updated successfully!");

		} catch (RuntimeException e) {
			log.warn("Validation error updating own profile: {}", e.getMessage());
			if (e.getMessage().contains("Username already exists") || e.getMessage().contains("Username '")) {
				result.rejectValue("username", "adminProfileDto.username", e.getMessage());
			} else if (e.getMessage().contains("Email already exists") || e.getMessage().contains("Email '")) {
				result.rejectValue("email", "adminProfileDto.email", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("adminError", "Error updating profile: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminProfileDto",
					result);
			redirectAttributes.addFlashAttribute("adminProfileDto", adminDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "editProfileModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/admins";
	}

	@PostMapping("/delete/{id}")
	@PreAuthorize("hasAuthority('DELETE_ADMINS')")
	public String deleteAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Principal principal) {
		Optional<User> userOpt = adminService.findUserById(id);
		if (userOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("adminError", "Admin not found or invalid ID.");
			return "redirect:/admin/admins";
		}
		String username = userOpt.get().getUsername();
		if (principal.getName().equals(username)) {
			redirectAttributes.addFlashAttribute("adminError", "You cannot delete your own account.");
			return "redirect:/admin/admins";
		}

		try {
			adminService.deleteAdminById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_ADMIN",
					"Deleted admin user: " + username + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("adminSuccess", "Admin '" + username + "' deleted successfully!");
		} catch (Exception e) {
			log.error("Error deleting admin ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("adminError", "Error deleting admin: " + e.getMessage());
		}
		return "redirect:/admin/admins";
	}
}