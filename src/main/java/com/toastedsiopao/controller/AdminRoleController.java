package com.toastedsiopao.controller;

import com.toastedsiopao.dto.RoleDto;
import com.toastedsiopao.model.Role;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.AdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Optional;
import java.util.stream.Collectors; // IMPORTED

@Controller
@RequestMapping("/admin/admins/roles")
@PreAuthorize("hasRole('OWNER')")
public class AdminRoleController {

	private static final Logger log = LoggerFactory.getLogger(AdminRoleController.class);

	@Autowired
	private AdminService adminService;

	@Autowired
	private ActivityLogService activityLogService;

	@PostMapping("/add")
	public String addRole(@Valid @ModelAttribute("roleDto") RoleDto roleDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			// --- MODIFIED: Simplified toast notification message ---
			String allErrors = result.getFieldErrors().stream()
					.map(err -> err.getField() + ": " + err.getDefaultMessage()).collect(Collectors.joining(", "));
			log.warn("Role creation validation failed: {}", allErrors);
			redirectAttributes.addFlashAttribute("globalError", "Validation failed. Please check the fields below.");
			// --- END MODIFICATION ---

			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.roleDto", result);
			redirectAttributes.addFlashAttribute("roleDto", roleDto);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "manageRolesModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			Role savedRole = adminService.createRole(roleDto);
			activityLogService.logAdminAction(principal.getName(), "ADD_ROLE",
					"Created new role: " + savedRole.getName());
			redirectAttributes.addFlashAttribute("adminSuccess",
					"Role '" + savedRole.getName() + "' created successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error creating role: {}", e.getMessage());

			// --- MODIFIED: Re-added rejectValue AND kept globalError ---
			if (e.getMessage().contains("Role name")) {
				result.rejectValue("name", "roleDto.name", e.getMessage());
			}
			redirectAttributes.addFlashAttribute("globalError", "Error creating role: " + e.getMessage());
			// --- END MODIFICATION ---

			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.roleDto", result);
			redirectAttributes.addFlashAttribute("roleDto", roleDto);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "manageRolesModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/admins";
	}

	@PostMapping("/update")
	public String updateRole(@Valid @ModelAttribute("roleUpdateDto") RoleDto roleDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			// --- MODIFIED: Simplified toast notification message ---
			String allErrors = result.getFieldErrors().stream()
					.map(err -> err.getField() + ": " + err.getDefaultMessage()).collect(Collectors.joining(", "));
			log.warn("Role update validation failed: {}", allErrors);
			redirectAttributes.addFlashAttribute("globalError", "Validation failed. Please check the fields below.");
			// --- END MODIFICATION ---

			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.roleUpdateDto", result);
			redirectAttributes.addFlashAttribute("roleUpdateDto", roleDto);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "editRoleModal")
					.queryParam("editId", roleDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			Role updatedRole = adminService.updateRole(roleDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_ROLE",
					"Updated role: " + updatedRole.getName() + " (ID: " + updatedRole.getId() + ")");
			redirectAttributes.addFlashAttribute("adminSuccess",
					"Role '" + updatedRole.getName() + "' updated successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error updating role: {}", e.getMessage());

			// --- MODIFIED: Re-added rejectValue AND kept globalError ---
			if (e.getMessage().contains("Role name")) {
				result.rejectValue("name", "roleUpdateDto.name", e.getMessage());
			}
			redirectAttributes.addFlashAttribute("globalError", "Error updating role: " + e.getMessage());
			// --- END MODIFICATION ---

			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.roleUpdateDto", result);
			redirectAttributes.addFlashAttribute("roleUpdateDto", roleDto);
			String redirectUrl = uriBuilder.path("/admin/admins").queryParam("showModal", "editRoleModal")
					.queryParam("editId", roleDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/admins";
	}

	@PostMapping("/delete/{id}")
	public String deleteRole(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Principal principal) {

		// --- REMOVED: try-catch block ---

		Optional<Role> roleOpt = adminService.findRoleById(id);
		if (roleOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("adminError", "Role not found or invalid ID.");
			return "redirect:/admin/admins";
		}
		String roleName = roleOpt.get().getName();

		// Let the service throw an exception if deletion fails
		// The GlobalExceptionHandler will catch it.
		adminService.deleteRole(id);

		activityLogService.logAdminAction(principal.getName(), "DELETE_ROLE",
				"Deleted role: " + roleName + " (ID: " + id + ")");
		redirectAttributes.addFlashAttribute("adminSuccess", "Role '" + roleName + "' deleted successfully!");

		return "redirect:/admin/admins";
	}
}