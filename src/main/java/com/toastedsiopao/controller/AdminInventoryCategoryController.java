package com.toastedsiopao.controller;

import com.toastedsiopao.dto.InventoryCategoryDto;
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.InventoryCategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize; // **** NEW IMPORT ****
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

@Controller
@RequestMapping("/admin/inventory/categories")
@PreAuthorize("hasAuthority('MANAGE_INVENTORY_CATEGORIES')") // **** ADDED: Secures all methods in this class ****
public class AdminInventoryCategoryController {

	private static final Logger log = LoggerFactory.getLogger(AdminInventoryCategoryController.class);

	@Autowired
	private InventoryCategoryService inventoryCategoryService;

	@Autowired
	private ActivityLogService activityLogService;

	// Helper to add common DTOs for modal forms
	@ModelAttribute("inventoryCategoryDto")
	public InventoryCategoryDto inventoryCategoryDto() {
		return new InventoryCategoryDto();
	}

	@ModelAttribute("inventoryCategoryUpdateDto")
	public InventoryCategoryDto inventoryCategoryUpdateDto() {
		return new InventoryCategoryDto();
	}

	@PostMapping("/add")
	public String addInventoryCategory(@Valid @ModelAttribute("inventoryCategoryDto") InventoryCategoryDto categoryDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryCategoryDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			// No need to add common attributes, redirect will trigger main controller's GET
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}
		try {
			InventoryCategory newCategory = inventoryCategoryService.saveFromDto(categoryDto);
			activityLogService.logAdminAction(principal.getName(), "ADD_INVENTORY_CATEGORY",
					"Added new inventory category: " + newCategory.getName());
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Inventory Category '" + newCategory.getName() + "' added successfully!");
		} catch (IllegalArgumentException e) {
			log.warn("Validation error adding inventory category: {}", e.getMessage());
			if (e.getMessage().contains("already exists")) {
				result.rejectValue("name", "duplicate", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("categoryError", "Error adding category: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryCategoryDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		} catch (RuntimeException e) {
			log.error("Error adding inventory category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/update")
	public String updateInventoryCategory(
			@Valid @ModelAttribute("inventoryCategoryUpdateDto") InventoryCategoryDto categoryDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			log.warn("Inventory category update DTO validation failed. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute(
					"org.springframework.validation.BindingResult.inventoryCategoryUpdateDto", result);
			redirectAttributes.addFlashAttribute("inventoryCategoryUpdateDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editInvCategoryModal")
					.queryParam("editId", categoryDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			InventoryCategory updatedCategory = inventoryCategoryService.updateFromDto(categoryDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_INVENTORY_CATEGORY",
					"Updated inventory category: " + updatedCategory.getName() + " (ID: " + updatedCategory.getId()
							+ ")");
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + updatedCategory.getName() + "' updated successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error updating inventory category: {}", e.getMessage());
			if (e.getMessage().contains("already exists")) {
				result.rejectValue("name", "duplicate", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("categoryError", "Error updating category: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute(
					"org.springframework.validation.BindingResult.inventoryCategoryUpdateDto", result);
			redirectAttributes.addFlashAttribute("inventoryCategoryUpdateDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editInvCategoryModal")
					.queryParam("editId", categoryDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (RuntimeException e) {
			log.error("Error updating inventory category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryCategoryUpdateDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editInvCategoryModal")
					.queryParam("editId", categoryDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/inventory";
	}

	@PostMapping("/delete/{id}")
	public String deleteInventoryCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<InventoryCategory> categoryOpt = inventoryCategoryService.findById(id);
		if (categoryOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Category not found.");
			return "redirect:/admin/inventory";
		}
		String categoryName = categoryOpt.get().getName();

		try {
			inventoryCategoryService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_CATEGORY",
					"Deleted inventory category: " + categoryName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + categoryName + "' deleted successfully!");
		} catch (RuntimeException e) {
			log.error("Error deleting inventory category ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "Error deleting category: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}
}