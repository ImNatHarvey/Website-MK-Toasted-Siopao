package com.toastedsiopao.controller;

import com.toastedsiopao.dto.CategoryDto;
import com.toastedsiopao.model.Category;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.CategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/admin/products/categories")
public class AdminProductCategoryController {

	private static final Logger log = LoggerFactory.getLogger(AdminProductCategoryController.class);

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ActivityLogService activityLogService;

	// Helper to add common DTOs for modal forms
	@ModelAttribute("categoryDto")
	public CategoryDto categoryDto() {
		return new CategoryDto();
	}

	@ModelAttribute("categoryUpdateDto")
	public CategoryDto categoryUpdateDto() {
		return new CategoryDto();
	}

	@PostMapping("/add")
	public String addCategory(@Valid @ModelAttribute("categoryDto") CategoryDto categoryDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoryDto", result);
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			Category newCategory = categoryService.saveFromDto(categoryDto);
			activityLogService.logAdminAction(principal.getName(), "ADD_CATEGORY",
					"Added new product category: " + newCategory.getName());
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + newCategory.getName() + "' added successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error adding product category: {}", e.getMessage());
			if (e.getMessage().contains("already exists")) {
				result.rejectValue("name", "duplicate", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("categoryError", "Error adding category: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoryDto", result);
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (RuntimeException e) {
			log.error("Error adding product category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("categoryDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/products";
	}

	@PostMapping("/update")
	public String updateCategory(@Valid @ModelAttribute("categoryUpdateDto") CategoryDto categoryDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			log.warn("Category update DTO validation failed. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoryUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("categoryUpdateDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "editCategoryModal")
					.queryParam("editId", categoryDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			Category updatedCategory = categoryService.updateFromDto(categoryDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_CATEGORY", "Updated product category: "
					+ updatedCategory.getName() + " (ID: " + updatedCategory.getId() + ")");
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + updatedCategory.getName() + "' updated successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error updating product category: {}", e.getMessage());
			if (e.getMessage().contains("already exists")) {
				result.rejectValue("name", "duplicate", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("categoryError", "Error updating category: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoryUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("categoryUpdateDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "editCategoryModal")
					.queryParam("editId", categoryDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (RuntimeException e) {
			log.error("Error updating product category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("categoryUpdateDto", categoryDto);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "editCategoryModal")
					.queryParam("editId", categoryDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/products";
	}

	@PostMapping("/delete/{id}")
	public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<Category> categoryOpt = categoryService.findById(id);
		if (categoryOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Category not found.");
			return "redirect:/admin/products";
		}
		String categoryName = categoryOpt.get().getName();

		try {
			categoryService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_CATEGORY",
					"Deleted product category: " + categoryName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + categoryName + "' deleted successfully!");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("categoryError", "Error deleting category: " + e.getMessage());
		}
		return "redirect:/admin/products";
	}
}