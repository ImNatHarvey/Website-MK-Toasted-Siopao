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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/inventory")
public class AdminInventoryController {

	// ... (Logger, Autowired fields, addCommonAttributesForRedirect, GetMapping,
	// saveInventoryItem, etc. are unchanged) ...
	private static final Logger log = LoggerFactory.getLogger(AdminInventoryController.class);

	@Autowired
	private InventoryItemService inventoryItemService;
	@Autowired
	private InventoryCategoryService inventoryCategoryService;
	@Autowired
	private UnitOfMeasureService unitOfMeasureService;
	@Autowired
	private ActivityLogService activityLogService;

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("inventoryCategories", inventoryCategoryService.findAll());
		redirectAttributes.addFlashAttribute("unitsOfMeasure", unitOfMeasureService.findAll());
		redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAll());
		redirectAttributes.addFlashAttribute("allInventoryItems", inventoryItemService.findAll());
	}

	// --- UPDATED METHOD ---
	@GetMapping
	public String manageInventory(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) { // Default size 10

		Pageable pageable = PageRequest.of(page, size);
		Page<InventoryItem> inventoryPage = inventoryItemService.searchItems(keyword, categoryId, pageable);

		List<InventoryCategory> categories = inventoryCategoryService.findAll();
		List<UnitOfMeasure> units = unitOfMeasureService.findAll();
		List<InventoryItem> allItemsForStockModal = inventoryItemService.findAll(); // Unchanged: modal needs all items

		// --- NEW: Calculate Stats ---
		// We get *all* items for stats calculation, not just the paged ones.
		List<InventoryItem> allItems = inventoryItemService.findAll();
		BigDecimal totalInventoryValue = allItems.stream().map(InventoryItem::getTotalCostValue) // Use the new method
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		List<InventoryItem> lowStockItems = inventoryItemService.findLowStockItems();
		List<InventoryItem> outOfStockItems = inventoryItemService.findOutOfStockItems();

		model.addAttribute("totalInventoryValue", totalInventoryValue);
		model.addAttribute("lowStockCount", lowStockItems.size());
		model.addAttribute("outOfStockCount", outOfStockItems.size());
		// totalItems is already available from inventoryPage.getTotalElements()
		// --- END NEW ---

		model.addAttribute("allInventoryItems", allItemsForStockModal);
		model.addAttribute("inventoryPage", inventoryPage); // Add the full page object
		model.addAttribute("inventoryItems", inventoryPage.getContent()); // Get content for current page
		model.addAttribute("inventoryCategories", categories);
		model.addAttribute("unitsOfMeasure", units);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategoryId", categoryId);

		// Pass pagination attributes to the model
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", inventoryPage.getTotalPages());
		model.addAttribute("totalItems", inventoryPage.getTotalElements());
		model.addAttribute("size", size);

		if (!model.containsAttribute("inventoryItemDto")) {
			model.addAttribute("inventoryItemDto", new InventoryItemDto());
		}
		if (!model.containsAttribute("inventoryCategoryDto")) {
			model.addAttribute("inventoryCategoryDto", new InventoryCategoryDto());
		}
		if (!model.containsAttribute("unitOfMeasureDto")) {
			model.addAttribute("unitOfMeasureDto", new UnitOfMeasureDto());
		}
		// --- NEW: Add DTO for category update modal ---
		if (!model.containsAttribute("inventoryCategoryUpdateDto")) {
			model.addAttribute("inventoryCategoryUpdateDto", new InventoryCategoryDto());
		}
		// --- NEW: Add DTO for unit update modal (for next step) ---
		if (!model.containsAttribute("unitOfMeasureUpdateDto")) {
			model.addAttribute("unitOfMeasureUpdateDto", new UnitOfMeasureDto());
		}
		// --- END NEW ---
		return "admin/inventory";
	}
	// --- END UPDATED METHOD ---

	@PostMapping("/save")
	public String saveInventoryItem(@Valid @ModelAttribute("inventoryItemDto") InventoryItemDto itemDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {
		if (result.hasErrors()) {
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
			InventoryItem savedItem = inventoryItemService.save(itemDto);
			String action = itemDto.getId() == null ? "ADD_INVENTORY_ITEM" : "EDIT_INVENTORY_ITEM";
			String message = itemDto.getId() == null ? "Added" : "Updated";
			activityLogService.logAdminAction(principal.getName(), action,
					message + " inventory item: " + savedItem.getName() + " (ID: " + savedItem.getId() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Item '" + savedItem.getName() + "' " + message.toLowerCase() + " successfully!");
		} catch (RuntimeException e) {
			log.warn("Error saving inventory item: {}", e.getMessage());
			if (e.getMessage().contains("already exists")) {
				result.addError(new FieldError("inventoryItemDto", "name", itemDto.getName(), false, null, null,
						e.getMessage()));
			} else if (e.getMessage().contains("threshold")) {
				result.reject("global", e.getMessage());
			} else {
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
		return "redirect:/admin/inventory";
	}

	@PostMapping("/categories/add")
	public String addInventoryCategory(@Valid @ModelAttribute("inventoryCategoryDto") InventoryCategoryDto categoryDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {
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
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		} catch (RuntimeException e) {
			log.error("Error adding inventory category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryCategoryDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageCategoriesModal")
					.build().toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/inventory";
	}

	// --- NEW: Update Inventory Category Method ---
	@PostMapping("/categories/update")
	public String updateInventoryCategory(
			@Valid @ModelAttribute("inventoryCategoryUpdateDto") InventoryCategoryDto categoryDto, BindingResult result,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		// Check DTO validation errors first
		if (result.hasErrors()) {
			log.warn("Inventory category update DTO validation failed. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute(
					"org.springframework.validation.BindingResult.inventoryCategoryUpdateDto", result);
			redirectAttributes.addFlashAttribute("inventoryCategoryUpdateDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editInvCategoryModal")
					.queryParam("editId", categoryDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles validation and updating
			InventoryCategory updatedCategory = inventoryCategoryService.updateFromDto(categoryDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_INVENTORY_CATEGORY",
					"Updated inventory category: " + updatedCategory.getName() + " (ID: " + updatedCategory.getId()
							+ ")");
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + updatedCategory.getName() + "' updated successfully!");

		} catch (IllegalArgumentException e) { // Catch validation errors from service
			log.warn("Validation error updating inventory category: {}", e.getMessage());
			if (e.getMessage().contains("already exists")) {
				result.rejectValue("name", "duplicate", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("categoryError", "Error updating category: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute(
					"org.springframework.validation.BindingResult.inventoryCategoryUpdateDto", result);
			redirectAttributes.addFlashAttribute("inventoryCategoryUpdateDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editInvCategoryModal")
					.queryParam("editId", categoryDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (RuntimeException e) { // Catch other runtime errors (e.g., not found)
			log.error("Error updating inventory category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("inventoryCategoryUpdateDto", categoryDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editInvCategoryModal")
					.queryParam("editId", categoryDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/inventory";
	}
	// --- END NEW ---

	@PostMapping("/units/add")
	public String addUnitOfMeasure(@Valid @ModelAttribute("unitOfMeasureDto") UnitOfMeasureDto unitDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {
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
			UnitOfMeasure newUnit = unitOfMeasureService.saveFromDto(unitDto);
			activityLogService.logAdminAction(principal.getName(), "ADD_UNIT_OF_MEASURE",
					"Added new unit: " + newUnit.getName() + " (" + newUnit.getAbbreviation() + ")");
			redirectAttributes.addFlashAttribute("unitSuccess", "Unit '" + newUnit.getName() + "' added successfully!");
		} catch (IllegalArgumentException e) {
			log.warn("Validation error adding unit: {}", e.getMessage());
			result.reject("global", e.getMessage());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageUnitsModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		} catch (RuntimeException e) {
			log.error("Error adding unit of measure: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("unitError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageUnitsModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/inventory";
	}

	// --- NEW: Update Unit Method ---
	@PostMapping("/units/update")
	public String updateUnitOfMeasure(@Valid @ModelAttribute("unitOfMeasureUpdateDto") UnitOfMeasureDto unitDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		// Check DTO validation errors first
		if (result.hasErrors()) {
			log.warn("Unit update DTO validation failed. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureUpdateDto", unitDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editUnitModal")
					.queryParam("editId", unitDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			// Service method now handles validation and updating
			UnitOfMeasure updatedUnit = unitOfMeasureService.updateFromDto(unitDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_UNIT_OF_MEASURE",
					"Updated unit: " + updatedUnit.getName() + " (ID: " + updatedUnit.getId() + ")");
			redirectAttributes.addFlashAttribute("unitSuccess",
					"Unit '" + updatedUnit.getName() + "' updated successfully!");

		} catch (IllegalArgumentException e) { // Catch validation errors from service
			log.warn("Validation error updating unit: {}", e.getMessage());
			// For units, duplicate errors are global
			result.reject("global", e.getMessage());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureUpdateDto", unitDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editUnitModal")
					.queryParam("editId", unitDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;

		} catch (RuntimeException e) { // Catch other runtime errors (e.g., not found)
			log.error("Error updating unit: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("unitError", "An unexpected error occurred: " + e.getMessage());
			redirectAttributes.addFlashAttribute("unitOfMeasureUpdateDto", unitDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editUnitModal")
					.queryParam("editId", unitDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/inventory";
	}
	// --- END NEW ---

	// **** METHOD UPDATED ****
	@PostMapping("/stock/adjust")
	public String adjustInventoryStock(@RequestParam("inventoryItemId") Long itemId,
			@RequestParam("quantity") BigDecimal quantity, // Changed from quantityChange
			@RequestParam("action") String action, // Added action parameter
			@RequestParam(value = "reason", required = false) String reason, RedirectAttributes redirectAttributes,
			Principal principal, UriComponentsBuilder uriBuilder) {

		// Validate that quantity is positive
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity must be a positive number.");
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		// Determine the final quantity change (positive or negative)
		BigDecimal quantityChange = action.equals("deduct") ? quantity.negate() : quantity;

		String finalReason = StringUtils.hasText(reason) ? reason
				: (action.equals("add") ? "Manual Stock Increase" : "Manual Stock Decrease/Wastage");

		try {
			InventoryItem updatedItem = inventoryItemService.adjustStock(itemId, quantityChange, finalReason);
			String actionText = action.equals("add") ? "Increased" : "Decreased";
			String details = actionText + " stock for " + updatedItem.getName() + " (ID: " + itemId + ") by "
					+ quantity.abs() + ". Reason: " + finalReason;
			redirectAttributes.addFlashAttribute("stockSuccess", actionText + " stock for '" + updatedItem.getName()
					+ "' by " + quantity.abs() + ". New stock: " + updatedItem.getCurrentStock());
			activityLogService.logAdminAction(principal.getName(), "ADJUST_INVENTORY_STOCK", details);
		} catch (RuntimeException e) {
			log.error("Error adjusting inventory stock for item ID {}: {}", itemId, e.getMessage());
			redirectAttributes.addFlashAttribute("stockError", "Error adjusting stock: " + e.getMessage());
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/inventory";
	}
	// **** END OF UPDATED METHOD ****

	@PostMapping("/delete/{id}")
	public String deleteInventoryItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
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
		Optional<InventoryCategory> categoryOpt = inventoryCategoryService.findById(id);
		if (categoryOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Category not found.");
			return "redirect:/admin/inventory";
		}
		InventoryCategory category = categoryOpt.get();
		// Use the new paged search method to check for items
		Page<InventoryItem> itemsInCategory = inventoryItemService.searchItems(null, id, PageRequest.of(0, 1));
		if (!itemsInCategory.isEmpty()) {
			redirectAttributes.addFlashAttribute("categoryError", "Cannot delete category '" + category.getName()
					+ "'. It is associated with " + itemsInCategory.getTotalElements() + " inventory item(s).");
			return "redirect:/admin/inventory";
		}
		try {
			String categoryName = category.getName();
			inventoryCategoryService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_CATEGORY",
					"Deleted inventory category: " + categoryName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("categorySuccess",
					"Category '" + categoryName + "' deleted successfully!");
		} catch (Exception e) {
			log.error("Error deleting inventory category ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("categoryError", "Error deleting category: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/units/delete/{id}")
	public String deleteUnitOfMeasure(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<UnitOfMeasure> unitOpt = unitOfMeasureService.findById(id);
		if (unitOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("unitError", "Unit not found.");
			return "redirect:/admin/inventory";
		}
		UnitOfMeasure unit = unitOpt.get();
		List<InventoryItem> itemsUsingUnit = inventoryItemService.findAll().stream()
				.filter(item -> item.getUnit() != null && item.getUnit().getId().equals(id))
				.collect(Collectors.toList());
		if (!itemsUsingUnit.isEmpty()) {
			redirectAttributes.addFlashAttribute("unitError", "Cannot delete unit '" + unit.getName()
					+ "'. It is associated with " + itemsUsingUnit.size() + " inventory item(s).");
			return "redirect:/admin/inventory";
		}
		try {
			String unitName = unit.getName();
			unitOfMeasureService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_UNIT_OF_MEASURE",
					"Deleted unit: " + unitName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("unitSuccess", "Unit '" + unitName + "' deleted successfully!");
		} catch (Exception e) {
			log.error("Error deleting unit ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("unitError", "Error deleting unit: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}
}