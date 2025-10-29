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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/inventory") // Base path for inventory-related actions
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

	@GetMapping
	public String manageInventory(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) Long categoryId) {
		List<InventoryItem> items = inventoryItemService.searchItems(keyword, categoryId);
		List<InventoryCategory> categories = inventoryCategoryService.findAll();
		List<UnitOfMeasure> units = unitOfMeasureService.findAll();

		// Get all items again specifically for the 'Manage Stock' modal
		List<InventoryItem> allItemsForStockModal = inventoryItemService.findAll();
		model.addAttribute("allInventoryItems", allItemsForStockModal);

		model.addAttribute("inventoryItems", items); // Filtered list for the main table
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
		return "admin/inventory"; // Renders inventory.html
	}

	@PostMapping("/save")
	public String saveInventoryItem(@Valid @ModelAttribute("inventoryItemDto") InventoryItemDto itemDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		// Custom validation: critical <= low
		if (itemDto.getCriticalStockThreshold() != null && itemDto.getLowStockThreshold() != null
				&& itemDto.getCriticalStockThreshold().compareTo(itemDto.getLowStockThreshold()) > 0) {
			result.rejectValue("criticalStockThreshold", "ThresholdError",
					"Critical threshold cannot be greater than low threshold.");
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryItemDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			redirectAttributes.addFlashAttribute("showAddItemModal", true);
			// Pass necessary lists back for the modal
			redirectAttributes.addFlashAttribute("inventoryCategories", inventoryCategoryService.findAll());
			redirectAttributes.addFlashAttribute("unitsOfMeasure", unitOfMeasureService.findAll());
			return "redirect:/admin/inventory"; // Corrected redirect path
		}

		try {
			InventoryItem savedItem = inventoryItemService.save(itemDto);
			String action = itemDto.getId() == null ? "ADD_INVENTORY_ITEM" : "EDIT_INVENTORY_ITEM";
			String message = itemDto.getId() == null ? "Added" : "Updated";

			activityLogService.logAdminAction(principal.getName(), action,
					message + " inventory item: " + savedItem.getName() + " (ID: " + savedItem.getId() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Item '" + savedItem.getName() + "' " + message.toLowerCase() + " successfully!");
		} catch (IllegalArgumentException e) { // Catch specific validation errors like duplicate name
			log.warn("Validation error saving inventory item: {}", e.getMessage()); // Log warning
			redirectAttributes.addFlashAttribute("inventoryError", "Error saving item: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto); // Send back DTO with errors
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryItemDto",
					result); // Include binding result if needed
			redirectAttributes.addFlashAttribute("showAddItemModal", true); // Reopen modal
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("inventoryCategories", inventoryCategoryService.findAll());
			redirectAttributes.addFlashAttribute("unitsOfMeasure", unitOfMeasureService.findAll());
		} catch (Exception e) {
			log.error("Unexpected error saving inventory item: {}", e.getMessage(), e); // Log error
			redirectAttributes.addFlashAttribute("inventoryError", "Error saving item: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			redirectAttributes.addFlashAttribute("showAddItemModal", true);
			// Pass necessary lists back
			redirectAttributes.addFlashAttribute("inventoryCategories", inventoryCategoryService.findAll());
			redirectAttributes.addFlashAttribute("unitsOfMeasure", unitOfMeasureService.findAll());
		}

		return "redirect:/admin/inventory"; // Corrected redirect path
	}

	@PostMapping("/delete/{id}")
	public String deleteInventoryItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		Optional<InventoryItem> itemOpt = inventoryItemService.findById(id);
		if (itemOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Item not found. Could not delete.");
			return "redirect:/admin/inventory"; // Corrected redirect path
		}

		String itemName = itemOpt.get().getName();

		try {
			inventoryItemService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_ITEM",
					"Deleted inventory item: " + itemName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess", "Item '" + itemName + "' deleted successfully!");
		} catch (RuntimeException e) { // Catch constraint violation or other runtime issues
			log.warn("Could not delete inventory item '{}': {}", itemName, e.getMessage()); // Log warning
			redirectAttributes.addFlashAttribute("inventoryError",
					"Could not delete item '" + itemName + "': " + e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error deleting inventory item '{}': {}", itemName, e.getMessage(), e); // Log error
			redirectAttributes.addFlashAttribute("inventoryError",
					"An unexpected error occurred while deleting item '" + itemName + "'.");
		}

		return "redirect:/admin/inventory"; // Corrected redirect path
	}

	@PostMapping("/stock/adjust")
	public String adjustInventoryStock(@RequestParam("inventoryItemId") Long itemId,
			@RequestParam("quantityChange") BigDecimal quantityChange, RedirectAttributes redirectAttributes,
			Principal principal) {

		String defaultReason = quantityChange.compareTo(BigDecimal.ZERO) > 0 ? "Manual Stock Increase"
				: "Manual Stock Decrease/Wastage";

		if (quantityChange.compareTo(BigDecimal.ZERO) == 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity change cannot be zero.");
			redirectAttributes.addFlashAttribute("showManageStockModal", true);
			return "redirect:/admin/inventory"; // Corrected redirect path
		}

		try {
			InventoryItem updatedItem = inventoryItemService.adjustStock(itemId, quantityChange, defaultReason);
			String action = quantityChange.compareTo(BigDecimal.ZERO) > 0 ? "Increased" : "Decreased";
			String details = action + " stock for " + updatedItem.getName() + " (ID: " + itemId + ") by "
					+ quantityChange.abs() + ". Reason: " + defaultReason;

			redirectAttributes.addFlashAttribute("stockSuccess", action + " stock for '" + updatedItem.getName()
					+ "' by " + quantityChange.abs() + ". New stock: " + updatedItem.getCurrentStock());

			activityLogService.logAdminAction(principal.getName(), "ADJUST_INVENTORY_STOCK", details);

		} catch (RuntimeException e) {
			log.error("Error adjusting inventory stock for item ID {}: {}", itemId, e.getMessage(), e); // Log error
			redirectAttributes.addFlashAttribute("stockError", "Error adjusting stock: " + e.getMessage());
			redirectAttributes.addFlashAttribute("showManageStockModal", true);
		}

		return "redirect:/admin/inventory"; // Corrected redirect path
	}

	@PostMapping("/categories/add")
	public String addInventoryCategory(@Valid @ModelAttribute("inventoryCategoryDto") InventoryCategoryDto categoryDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (categoryDto.getName() != null && !categoryDto.getName().trim().isEmpty()) {
			if (inventoryCategoryService.findByName(categoryDto.getName().trim()).isPresent()) {
				result.rejectValue("name", "duplicate", "Category name already exists");
			}
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryCategoryDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showManageCategoriesModal", true);
			return "redirect:/admin/inventory"; // Corrected redirect path
		}

		try {
			InventoryCategory newCategory = new InventoryCategory(categoryDto.getName().trim());
			inventoryCategoryService.save(newCategory);
			activityLogService.logAdminAction(principal.getName(), "ADD_INVENTORY_CATEGORY",
					"Added new inventory category: " + newCategory.getName());
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Inventory Category '" + newCategory.getName() + "' added successfully!");
		} catch (Exception e) {
			log.error("Error adding inventory category: {}", e.getMessage(), e); // Log error
			redirectAttributes.addFlashAttribute("inventoryError", "Error adding category: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			redirectAttributes.addFlashAttribute("showManageCategoriesModal", true);
		}
		return "redirect:/admin/inventory"; // Corrected redirect path
	}

	@PostMapping("/categories/delete/{id}")
	public String deleteInventoryCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		Optional<InventoryCategory> categoryOpt = inventoryCategoryService.findById(id);
		if (categoryOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Category not found.");
			return "redirect:/admin/inventory"; // Corrected redirect path
		}

		InventoryCategory category = categoryOpt.get();

		// Check if associated items exist
		List<InventoryItem> itemsInCategory = inventoryItemService.searchItems(null, id);
		if (!itemsInCategory.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Cannot delete '" + category.getName()
					+ "'. It is associated with " + itemsInCategory.size() + " inventory item(s).");
			return "redirect:/admin/inventory"; // Corrected redirect path
		}

		try {
			String categoryName = category.getName();
			inventoryCategoryService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_CATEGORY",
					"Deleted inventory category: " + categoryName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Category '" + categoryName + "' deleted successfully!");
		} catch (Exception e) {
			log.error("Error deleting inventory category: {}", e.getMessage(), e); // Log error
			redirectAttributes.addFlashAttribute("inventoryError", "Error deleting category: " + e.getMessage());
		}
		return "redirect:/admin/inventory"; // Corrected redirect path
	}

	@PostMapping("/units/add")
	public String addUnitOfMeasure(@Valid @ModelAttribute("unitOfMeasureDto") UnitOfMeasureDto unitDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal) {

		if (!result.hasFieldErrors("name") && !result.hasFieldErrors("abbreviation")) {
			Optional<UnitOfMeasure> existing = unitOfMeasureService.findByNameOrAbbreviation(unitDto.getName().trim(),
					unitDto.getAbbreviation().trim());
			if (existing.isPresent()) {
				result.reject("global", "A unit with this name or abbreviation already exists.");
			}
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			redirectAttributes.addFlashAttribute("showManageUnitsModal", true);
			return "redirect:/admin/inventory"; // Corrected redirect path
		}

		try {
			UnitOfMeasure newUnit = new UnitOfMeasure(unitDto.getName().trim(), unitDto.getAbbreviation().trim());
			unitOfMeasureService.save(newUnit);
			activityLogService.logAdminAction(principal.getName(), "ADD_UNIT_OF_MEASURE",
					"Added new unit: " + newUnit.getName() + " (" + newUnit.getAbbreviation() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Unit '" + newUnit.getName() + "' added successfully!");
		} catch (Exception e) {
			log.error("Error adding unit of measure: {}", e.getMessage(), e); // Log error
			redirectAttributes.addFlashAttribute("inventoryError", "Error adding unit: " + e.getMessage());
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			redirectAttributes.addFlashAttribute("showManageUnitsModal", true);
		}
		return "redirect:/admin/inventory"; // Corrected redirect path
	}

	@PostMapping("/units/delete/{id}")
	public String deleteUnitOfMeasure(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		Optional<UnitOfMeasure> unitOpt = unitOfMeasureService.findById(id);
		if (unitOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Unit not found.");
			return "redirect:/admin/inventory"; // Corrected redirect path
		}

		UnitOfMeasure unit = unitOpt.get();

		// Check if associated items exist
		List<InventoryItem> itemsUsingUnit = inventoryItemService.findAll().stream()
				.filter(item -> item.getUnit() != null && item.getUnit().getId().equals(id))
				.collect(Collectors.toList());

		if (!itemsUsingUnit.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Cannot delete '" + unit.getName()
					+ "'. It is associated with " + itemsUsingUnit.size() + " inventory item(s).");
			return "redirect:/admin/inventory"; // Corrected redirect path
		}

		try {
			String unitName = unit.getName();
			unitOfMeasureService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_UNIT_OF_MEASURE",
					"Deleted unit: " + unitName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess", "Unit '" + unitName + "' deleted successfully!");
		} catch (Exception e) {
			log.error("Error deleting unit of measure: {}", e.getMessage(), e); // Log error
			redirectAttributes.addFlashAttribute("inventoryError", "Error deleting unit: " + e.getMessage());
		}
		return "redirect:/admin/inventory"; // Corrected redirect path
	}
}