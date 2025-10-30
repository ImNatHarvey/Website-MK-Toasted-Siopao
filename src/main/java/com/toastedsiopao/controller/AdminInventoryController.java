package com.toastedsiopao.controller;

import com.toastedsiopao.dto.InventoryCategoryDto;
import com.toastedsiopao.dto.InventoryItemDto;
import com.toastedsiopao.dto.UnitOfMeasureDto;
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.UnitOfMeasure;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.InventoryCategoryService;
import com.toastedsiopao.service.InventoryItemService;
import com.toastedsiopao.service.UnitOfMeasureService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.StringUtils; // **** ADDED IMPORT ****

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/inventory")
public class AdminInventoryController {

	private static final Logger log = LoggerFactory.getLogger(AdminInventoryController.class);

	@Autowired
	private InventoryItemService inventoryItemService;
	@Autowired
	private InventoryCategoryService inventoryCategoryService;
	@Autowired
	private UnitOfMeasureService unitOfMeasureService;
	@Autowired
	private ActivityLogService activityLogService;

	// Helper remains the same
	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("inventoryCategories", inventoryCategoryService.findAll());
		redirectAttributes.addFlashAttribute("unitsOfMeasure", unitOfMeasureService.findAll());
		redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAll());
		redirectAttributes.addFlashAttribute("allInventoryItems", inventoryItemService.findAll());
	}

	@GetMapping // Unchanged
	public String manageInventory(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) Long categoryId) {
		// ... unchanged ...
		List<InventoryItem> items = inventoryItemService.searchItems(keyword, categoryId);
		List<InventoryCategory> categories = inventoryCategoryService.findAll();
		List<UnitOfMeasure> units = unitOfMeasureService.findAll();
		List<InventoryItem> allItemsForStockModal = inventoryItemService.findAll(); // Get all for modal

		model.addAttribute("allInventoryItems", allItemsForStockModal); // Add for stock modal
		model.addAttribute("inventoryItems", items); // Filtered items for main table
		model.addAttribute("inventoryCategories", categories);
		model.addAttribute("unitsOfMeasure", units);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategoryId", categoryId);
		if (!model.containsAttribute("inventoryItemDto")) {
			model.addAttribute("inventoryItemDto", new InventoryItemDto());
		}
		if (!model.containsAttribute("inventoryCategoryDto")) {
			model.addAttribute("inventoryCategoryDto", new InventoryCategoryDto());
		}
		if (!model.containsAttribute("unitOfMeasureDto")) {
			model.addAttribute("unitOfMeasureDto", new UnitOfMeasureDto());
		}
		return "admin/inventory";
	}

	@PostMapping("/save") // Unchanged from previous batch correction
	public String saveInventoryItem(@Valid @ModelAttribute("inventoryItemDto") InventoryItemDto itemDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {
		// DTO validation check
		if (result.hasErrors()) {
			// ... unchanged error handling ...
			log.warn("Inventory item DTO validation failed. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryItemDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "addItemModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		try {
			// Service handles validation
			InventoryItem savedItem = inventoryItemService.save(itemDto);
			// ... unchanged success logging & redirect ...
			String action = itemDto.getId() == null ? "ADD_INVENTORY_ITEM" : "EDIT_INVENTORY_ITEM";
			String message = itemDto.getId() == null ? "Added" : "Updated";
			activityLogService.logAdminAction(principal.getName(), action,
					message + " inventory item: " + savedItem.getName() + " (ID: " + savedItem.getId() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Item '" + savedItem.getName() + "' " + message.toLowerCase() + " successfully!");
		} catch (RuntimeException e) { // Catch validation and other runtime errors from service
			log.warn("Error saving inventory item: {}", e.getMessage());
			// Add specific error back to BindingResult or use a generic flash attribute
			if (e.getMessage().contains("already exists")) {
				result.addError(new FieldError("inventoryItemDto", "name", itemDto.getName(), false, null, null,
						e.getMessage()));
			} else if (e.getMessage().contains("threshold")) {
				result.reject("global", e.getMessage()); // Use global error for threshold issue
			} else {
				// Generic error for other validation issues from service
				redirectAttributes.addFlashAttribute("inventoryError", "Error saving item: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryItemDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "addItemModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		// Removed the separate Exception catch as RuntimeException covers others too
		return "redirect:/admin/inventory";
	}

	@PostMapping("/categories/add")
	public String addInventoryCategory(@Valid @ModelAttribute("inventoryCategoryDto") InventoryCategoryDto categoryDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		// --- Removed manual duplicate name check ---

		// Check only DTO validation errors first
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryCategoryDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles validation and saving
			InventoryCategory newCategory = inventoryCategoryService.saveFromDto(categoryDto); // Assuming a new method
																								// in service
			activityLogService.logAdminAction(principal.getName(), "ADD_INVENTORY_CATEGORY",
					"Added new inventory category: " + newCategory.getName());
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Inventory Category '" + newCategory.getName() + "' added successfully!");

		} catch (IllegalArgumentException e) { // Catch validation errors from service
			log.warn("Validation error adding inventory category: {}", e.getMessage());
			if (e.getMessage().contains("already exists")) {
				result.rejectValue("name", "duplicate", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("inventoryError", "Error adding category: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryCategoryDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (RuntimeException e) { // Catch other runtime errors during save
			log.error("Error adding inventory category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("inventoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/units/add")
	public String addUnitOfMeasure(@Valid @ModelAttribute("unitOfMeasureDto") UnitOfMeasureDto unitDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		// --- Removed manual duplicate name/abbr check ---

		// Check only DTO validation errors first (also covers global errors added by
		// DTO validation)
		if (result.hasErrors() || result.hasGlobalErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageUnitsModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles validation and saving
			UnitOfMeasure newUnit = unitOfMeasureService.saveFromDto(unitDto); // Assuming a new method in service
			activityLogService.logAdminAction(principal.getName(), "ADD_UNIT_OF_MEASURE",
					"Added new unit: " + newUnit.getName() + " (" + newUnit.getAbbreviation() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Unit '" + newUnit.getName() + "' added successfully!");

		} catch (IllegalArgumentException e) { // Catch validation errors from service
			log.warn("Validation error adding unit: {}", e.getMessage());
			// Service should throw specific message for duplicates
			result.reject("global", e.getMessage()); // Use global error for duplicate name/abbr
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageUnitsModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;

		} catch (RuntimeException e) { // Catch other runtime errors
			log.error("Error adding unit of measure: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("inventoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageUnitsModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/inventory";
	}

	// Stock Adjust POST mapping remains unchanged
	@PostMapping("/stock/adjust")
	public String adjustInventoryStock(@RequestParam("inventoryItemId") Long itemId,
			@RequestParam("quantityChange") BigDecimal quantityChange,
			@RequestParam(value = "reason", required = false) String reason, // Allow reason override
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		String finalReason = StringUtils.hasText(reason) ? reason
				: (quantityChange.compareTo(BigDecimal.ZERO) > 0 ? "Manual Stock Increase"
						: "Manual Stock Decrease/Wastage");

		if (quantityChange.compareTo(BigDecimal.ZERO) == 0) {
			// ... unchanged zero quantity error handling ...
			redirectAttributes.addFlashAttribute("stockError", "Quantity change cannot be zero.");
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		try {
			// ... unchanged success handling ...
			InventoryItem updatedItem = inventoryItemService.adjustStock(itemId, quantityChange, finalReason);
			String action = quantityChange.compareTo(BigDecimal.ZERO) > 0 ? "Increased" : "Decreased";
			String details = action + " stock for " + updatedItem.getName() + " (ID: " + itemId + ") by "
					+ quantityChange.abs() + ". Reason: " + finalReason;
			redirectAttributes.addFlashAttribute("stockSuccess", action + " stock for '" + updatedItem.getName()
					+ "' by " + quantityChange.abs() + ". New stock: " + updatedItem.getCurrentStock());
			activityLogService.logAdminAction(principal.getName(), "ADJUST_INVENTORY_STOCK", details);
		} catch (RuntimeException e) { // Catch validation and other runtime errors
			// ... unchanged error handling ...
			log.error("Error adjusting inventory stock for item ID {}: {}", itemId, e.getMessage());
			redirectAttributes.addFlashAttribute("stockError", "Error adjusting stock: " + e.getMessage());
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		// Removed separate Exception catch
		return "redirect:/admin/inventory";
	}

	// --- Delete methods remain unchanged ---
	@PostMapping("/delete/{id}")
	public String deleteInventoryItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		// ... unchanged delete logic ...
		Optional<InventoryItem> itemOpt = inventoryItemService.findById(id);
		if (itemOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Inventory item not found.");
			return "redirect:/admin/inventory";
		}
		String itemName = itemOpt.get().getName();
		try {
			inventoryItemService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_ITEM",
					"Deleted inventory item: " + itemName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess", "Item '" + itemName + "' deleted successfully!");
		} catch (RuntimeException e) {
			log.warn("Could not delete item '{}': {}", itemName, e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryError",
					"Could not delete item '" + itemName + "': " + e.getMessage());
		} catch (Exception e) {
			log.error("Error deleting inventory item ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("inventoryError",
					"An unexpected error occurred while deleting the item.");
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/categories/delete/{id}")
	public String deleteInventoryCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		// ... unchanged delete logic ...
		Optional<InventoryCategory> categoryOpt = inventoryCategoryService.findById(id);
		if (categoryOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Category not found.");
			return "redirect:/admin/inventory";
		}
		InventoryCategory category = categoryOpt.get();
		List<InventoryItem> itemsInCategory = inventoryItemService.searchItems(null, id);
		if (!itemsInCategory.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Cannot delete category '" + category.getName()
					+ "'. It is associated with " + itemsInCategory.size() + " inventory item(s).");
			return "redirect:/admin/inventory";
		}
		try {
			String categoryName = category.getName();
			inventoryCategoryService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_CATEGORY",
					"Deleted inventory category: " + categoryName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Category '" + categoryName + "' deleted successfully!");
		} catch (Exception e) {
			log.error("Error deleting inventory category ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("inventoryError", "Error deleting category: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/units/delete/{id}")
	public String deleteUnitOfMeasure(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		// ... unchanged delete logic ...
		Optional<UnitOfMeasure> unitOpt = unitOfMeasureService.findById(id);
		if (unitOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Unit not found.");
			return "redirect:/admin/inventory";
		}
		UnitOfMeasure unit = unitOpt.get();
		List<InventoryItem> itemsUsingUnit = inventoryItemService.findAll().stream()
				.filter(item -> item.getUnit() != null && item.getUnit().getId().equals(id))
				.collect(Collectors.toList());
		if (!itemsUsingUnit.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Cannot delete unit '" + unit.getName()
					+ "'. It is associated with " + itemsUsingUnit.size() + " inventory item(s).");
			return "redirect:/admin/inventory";
		}
		try {
			String unitName = unit.getName();
			unitOfMeasureService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_UNIT_OF_MEASURE",
					"Deleted unit: " + unitName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess", "Unit '" + unitName + "' deleted successfully!");
		} catch (Exception e) {
			log.error("Error deleting unit ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("inventoryError", "Error deleting unit: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}
}