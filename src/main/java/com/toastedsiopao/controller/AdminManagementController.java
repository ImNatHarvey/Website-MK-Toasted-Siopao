package com.toastedsiopao.controller;

import com.toastedsiopao.dto.AdminUpdateDto;
import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.model.Role; // NEW IMPORT
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
import org.springframework.security.access.prepost.PreAuthorize; // **** NEW IMPORT ****
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.List; // NEW IMPORT
import java.util.Optional;

@Controller
@RequestMapping("/admin/admins")
public class AdminManagementController {

	private static final Logger log = LoggerFactory.getLogger(AdminManagementController.class);

	@Autowired
	private AdminService adminService;

	@Autowired
	private CustomerService customerService; // For findByUsername

	@Autowired
	private ActivityLogService activityLogService;

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		Pageable defaultPageable = PageRequest.of(0, 10);
		redirectAttributes.addFlashAttribute("admins", adminService.findAllAdmins(defaultPageable).getContent());
		// redirectAttributes.addFlashAttribute("allAdminRoles",
		// adminService.findAllAdminRoles()); // REMOVED
	}

	@GetMapping
	@PreAuthorize("hasAuthority('VIEW_ADMINS')") // **** ADDED ****
	public String manageAdmins(Model model, Principal principal,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<User> adminPage = adminService.searchAdmins(keyword, pageable);
		// List<Role> allAdminRoles = adminService.findAllAdminRoles(); // REMOVED

		model.addAttribute("adminPage", adminPage);
		model.addAttribute("admins", adminPage.getContent());
		model.addAttribute("currentUsername", principal.getName());
		model.addAttribute("totalItems", adminPage.getTotalElements());
		model.addAttribute("activeAdminCount", adminService.countActiveAdmins());
		// model.addAttribute("allAdminRoles", allAdminRoles); // REMOVED

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", adminPage.getTotalPages());
		model.addAttribute("totalItems", adminPage.getTotalElements());
		model.addAttribute("size", size);
		model.addAttribute("keyword", keyword);

		// --- UPDATED: Use consistent DTO name ---
		if (!model.containsAttribute("adminAccountCreateDto")) {
			model.addAttribute("adminAccountCreateDto", new AdminAccountCreateDto());
		}
		// --- END UPDATE ---

		if (!model.containsAttribute("adminUpdateDto")) {
			model.addAttribute("adminUpdateDto", new AdminUpdateDto());
		}

		// --- NEW: Populate Edit Profile DTO ---
		if (!model.containsAttribute("adminProfileDto")) {
			User currentUser = customerService.findByUsername(principal.getName());
			AdminUpdateDto profileDto = new AdminUpdateDto();
			if (currentUser != null) {
				profileDto.setId(currentUser.getId());
				profileDto.setFirstName(currentUser.getFirstName());
				profileDto.setLastName(currentUser.getLastName());
				profileDto.setUsername(currentUser.getUsername());
				profileDto.setEmail(currentUser.getEmail());
				if (currentUser.getRole() != null) { // NEW
					// profileDto.setRoleId(currentUser.getRole().getId()); // REMOVED
					profileDto.setRoleName(currentUser.getRole().getName().replace("ROLE_", "")); // NEW
				}
			}
			model.addAttribute("adminProfileDto", profileDto);
		}
		// --- END NEW ---

		return "admin/admins";
	}

	// --- UPDATED: Use consistent DTO name ---
	@PostMapping("/add")
	@PreAuthorize("hasAuthority('ADD_ADMINS')") // **** ADDED ****
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
		// --- END UPDATE ---

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
			// --- UPDATED: Use consistent DTO name ---
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminAccountCreateDto",
					result);
			redirectAttributes.addFlashAttribute("adminAccountCreateDto", adminDto);
			// --- END UPDATE ---
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "addAdminModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/admins";
	}

	@PostMapping("/update")
	@PreAuthorize("hasAuthority('EDIT_ADMINS')") // **** ADDED ****
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

		} catch (RuntimeException e) { // **** CATCH RUNTIME EXCEPTION ****
			log.warn("Validation error updating admin: {}", e.getMessage());
			if (e.getMessage().contains("Username already exists") || e.getMessage().contains("Username '")) {
				result.rejectValue("username", "adminUpdateDto.username", e.getMessage());
			} else if (e.getMessage().contains("Email already exists") || e.getMessage().contains("Email '")) {
				result.rejectValue("email", "adminUpdateDto.email", e.getMessage());
			} else if (e.getMessage().contains("Role name")) { // NEW
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
	@PreAuthorize("isAuthenticated()") // **** ADDED: Any authenticated admin can edit their own profile ****
	public String updateAdminProfile(@Valid @ModelAttribute("adminProfileDto") AdminUpdateDto adminDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		// --- Manually clear errors for fields not in this form ---
		// This is a workaround because we are reusing a DTO that has more fields
		// than the form.
		if (result.hasFieldErrors("roleName")) {
			// We can't easily remove the error, but this is a sign of the problem.
			// The updateAdminProfile service method will ignore this field anyway.
		}
		// --- End Workaround ---

		if (result.hasErrors()) {
			// Check if the only errors are the ones we expect to ignore
			boolean onlyRoleErrors = result.getFieldErrors().stream().allMatch(e -> e.getField().equals("roleName"));

			if (!onlyRoleErrors) {
				redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adminProfileDto",
						result);
				redirectAttributes.addFlashAttribute("adminProfileDto", adminDto);
				addCommonAttributesForRedirect(redirectAttributes);
				String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "editProfileModal")
						.build().toUriString();
				return "redirect:" + redirectUrl;
			}
			log.warn("Ignoring 'roleName' validation error for self-profile update.");
		}

		// Security check: ensure the ID in the DTO matches the logged-in user
		User currentUser = customerService.findByUsername(principal.getName());
		if (currentUser == null || !currentUser.getId().equals(adminDto.getId())) {
			redirectAttributes.addFlashAttribute("adminError", "Security error: Profile mismatch.");
			return "redirect:/admin/admins";
		}

		// --- NEW: Manually set roleId for Owner profile update ---
		// This ensures the service-layer validation passes for the Owner
		if (currentUser.getRole() != null) {
			// adminDto.setRoleId(currentUser.getRole().getId()); // REMOVED
			adminDto.setRoleName(currentUser.getRole().getName().replace("ROLE_", "")); // NEW
		}
		// --- END NEW ---

		try {
			User updatedUser = adminService.updateAdminProfile(adminDto); // Uses same logic as updateAdmin for now
			activityLogService.logAdminAction(principal.getName(), "EDIT_PROFILE",
					"Updated own profile: " + updatedUser.getUsername());
			redirectAttributes.addFlashAttribute("adminSuccess", "Your profile has been updated successfully!");

		} catch (RuntimeException e) { // **** CATCH RUNTIME EXCEPTION ****
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
	@PreAuthorize("hasAuthority('DELETE_ADMINS')") // **** ADDED ****
	public String deleteAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Principal principal) {
		Optional<User> userOpt = adminService.findUserById(id);
		if (userOpt.isEmpty()) { // UPDATED: Check for empty optional
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