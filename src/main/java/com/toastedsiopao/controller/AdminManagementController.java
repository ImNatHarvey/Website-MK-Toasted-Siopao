package com.toastedsiopao.controller;

import com.toastedsiopao.dto.AdminProfileUpdateDto;
import com.toastedsiopao.dto.AdminUpdateDto;
import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.dto.RoleDto;
import com.toastedsiopao.model.Role;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.AdminService;
import com.toastedsiopao.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest; // IMPORT ADDED
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // IMPORT ADDED
import org.springframework.security.core.context.SecurityContextHolder; // IMPORT ADDED
import org.springframework.security.core.userdetails.UserDetails; // IMPORT ADDED
import org.springframework.security.core.userdetails.UserDetailsService; // IMPORT ADDED
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; // IMPORT ADDED
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

	// IMPORT ADDED
	@Autowired
	private UserDetailsService userDetailsService;

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		Pageable defaultPageable = PageRequest.of(0, 10);
		redirectAttributes.addFlashAttribute("admins", adminService.findAllAdmins(defaultPageable).getContent());
		redirectAttributes.addFlashAttribute("allAdminRoles", adminService.findAllAdminRoles());
	}

	@GetMapping
	@PreAuthorize("hasAuthority('VIEW_ADMINS')")
	public String manageAdmins(Model model, Principal principal,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<User> adminPage = adminService.searchAdmins(keyword, pageable);

		List<Role> allAdminRoles = adminService.findAllAdminRoles();

		model.addAttribute("adminPage", adminPage);
		model.addAttribute("admins", adminPage.getContent());
		model.addAttribute("allAdminRoles", allAdminRoles);
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

		// --- ADDED FOR NEW ROLE MODALS ---
		if (!model.containsAttribute("roleDto")) {
			model.addAttribute("roleDto", new RoleDto());
		}
		if (!model.containsAttribute("roleUpdateDto")) {
			model.addAttribute("roleUpdateDto", new RoleDto());
		}
		// --- END ADDED ---

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
	@PreAuthorize("hasAuthority('ADD_ADMINS')") // --- UPDATED ---
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
			User savedUser = adminService.createAccount(adminDto);
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
			} else if (e.getMessage().contains("role")) {
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
	@PreAuthorize("hasAuthority('EDIT_ADMINS')") // --- UPDATED ---
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
			} else if (e.getMessage().contains("role")) {
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
			UriComponentsBuilder uriBuilder, HttpServletRequest request) { // MODIFIED SIGNATURE

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

			// --- CODE BLOCK TO REFRESH SESSION ---
			// Manually update the security principal so the navbar reflects the change
			UserDetails userDetails = userDetailsService.loadUserByUsername(updatedUser.getUsername());
			UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(userDetails, null, // Credentials
																														// are
																														// not
																														// needed
																														// post-auth
					userDetails.getAuthorities());
			newAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(newAuth);
			log.info("Updated security context for user: {}", updatedUser.getUsername());
			// --- END OF BLOCK ---

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
	@PreAuthorize("hasAuthority('DELETE_ADMINS')") // --- UPDATED ---
	public String deleteAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Principal principal) {

		// --- REMOVED: try-catch block ---

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

		// Let the service throw an exception if deletion fails
		// The GlobalExceptionHandler will catch it.
		adminService.deleteAdminById(id);

		activityLogService.logAdminAction(principal.getName(), "DELETE_ADMIN",
				"Deleted admin user: " + username + " (ID: " + id + ")");
		redirectAttributes.addFlashAttribute("adminSuccess", "Admin '" + username + "' deleted successfully!");

		return "redirect:/admin/admins";
	}
}