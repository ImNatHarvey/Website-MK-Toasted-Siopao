package com.toastedsiopao.controller;

import com.toastedsiopao.dto.InventoryCategoryDto;
import com.toastedsiopao.dto.InventoryItemDto;
import com.toastedsiopao.dto.UnitOfMeasureDto;
import com.toastedsiopao.model.ActivityLogEntry;
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.UnitOfMeasure;
import com.toastedsiopao.repository.RecipeIngredientRepository;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.AdminService;
import com.toastedsiopao.service.InventoryCategoryService;
import com.toastedsiopao.service.InventoryItemService;
import com.toastedsiopao.service.UnitOfMeasureService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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

	@Autowired
	private RecipeIngredientRepository recipeIngredientRepository;

	@Autowired
	private AdminService adminService;

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("inventoryCategories", inventoryCategoryService.findAll());
		redirectAttributes.addFlashAttribute("unitsOfMeasure", unitOfMeasureService.findAll());
		redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAll());
		redirectAttributes.addFlashAttribute("allInventoryItems", inventoryItemService.findAllActive());
	}

	@GetMapping
	@PreAuthorize("hasAuthority('VIEW_INVENTORY')")
	public String manageInventory(Model model, 
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			// --- ADDED: Pass inventory list pagination to waste list form submit ---
			@RequestParam(value = "activeTab", required = false) String activeTab, 
			// --- MODIFIED: Renamed wasteKeyword -> keyword, added wasteCategory ---
			@RequestParam(value = "wasteKeyword", required = false) String wasteKeyword,
			@RequestParam(value = "wasteCategory", required = false) String wasteCategory, 
			@RequestParam(value = "wastePage", defaultValue = "0") int wastePage) {
			// --- END MODIFIED ---

		Pageable pageable = PageRequest.of(page, size);
		Page<InventoryItem> inventoryPage = inventoryItemService.searchItems(keyword, categoryId, pageable);
		List<InventoryCategory> categories = inventoryCategoryService.findAll();
		List<UnitOfMeasure> units = unitOfMeasureService.findAll();
		List<InventoryItem> allItemsForStockModal = inventoryItemService.findAllActive();
		List<InventoryItem> allItems = inventoryItemService.findAll();
		BigDecimal totalInventoryValue = allItems.stream().map(InventoryItem::getTotalCostValue).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		List<InventoryItem> lowStockItems = inventoryItemService.findLowStockItems();
		List<InventoryItem> outOfStockItems = inventoryItemService.findOutOfStockItems();

		// --- MODIFIED: Waste Log Fetching ---
		Pageable wastePageable = PageRequest.of(wastePage, size); 
		Page<ActivityLogEntry> wasteLogPage = activityLogService.searchWasteLogs(wasteKeyword, wasteCategory, wastePageable); // --- MODIFIED: Pass String wasteCategory ---
		model.addAttribute("wasteLogs", wasteLogPage.getContent());
		model.addAttribute("wasteLogPage", wasteLogPage);
		model.addAttribute("wasteKeyword", wasteKeyword);
		model.addAttribute("wasteCategoryId", wasteCategory); // --- MODIFIED: Pass as String for select box ---
		model.addAttribute("wastePage", wastePage);
		// ---------------------------------------------

		model.addAttribute("totalInventoryValue", totalInventoryValue);
		model.addAttribute("lowStockCount", lowStockItems.size());
		model.addAttribute("outOfStockCount", outOfStockItems.size());

		model.addAttribute("allInventoryItems", allItemsForStockModal);
		model.addAttribute("inventoryPage", inventoryPage);
		model.addAttribute("inventoryItems", inventoryPage.getContent());
		model.addAttribute("inventoryCategories", categories);
		model.addAttribute("unitsOfMeasure", units);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategoryId", categoryId);

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
		if (!model.containsAttribute("inventoryCategoryUpdateDto")) {
			model.addAttribute("inventoryCategoryUpdateDto", new InventoryCategoryDto());
		}
		if (!model.containsAttribute("unitOfMeasureUpdateDto")) {
			model.addAttribute("unitOfMeasureUpdateDto", new UnitOfMeasureDto());
		}
		return "admin/inventory";
	}

	@PostMapping("/save")
	@PreAuthorize("hasAuthority('ADD_INVENTORY_ITEMS') or hasAuthority('EDIT_INVENTORY_ITEMS')")
	public String saveInventoryItem(@Valid @ModelAttribute("inventoryItemDto") InventoryItemDto itemDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {
		
		boolean isNew = itemDto.getId() == null;
		
		if (result.hasErrors()) {
			log.warn("Inventory item DTO validation failed. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("globalError", "Validation failed. Please check the fields below.");
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryItemDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			addCommonAttributesForRedirect(redirectAttributes);
			
			String modal = "addItemModal";
			UriComponentsBuilder builder = uriBuilder.path("/admin/inventory").queryParam("showModal", modal);
			
			if (!isNew && itemDto.getId() != null) {
				builder = builder.queryParam("editId", itemDto.getId());
			}
			String redirectUrl = builder.build().toUriString();
			return "redirect:" + redirectUrl;
		}
		try {
			InventoryItem savedItem = inventoryItemService.save(itemDto);
			String action = isNew ? "ADD_INVENTORY_ITEM" : "EDIT_INVENTORY_ITEM";
			String message = isNew ? "Added" : "Updated";
			activityLogService.logAdminAction(principal.getName(), action,
					message + " inventory item: " + savedItem.getName() + " (ID: " + savedItem.getId() + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Item '" + savedItem.getName() + "' " + message.toLowerCase() + " successfully!");
		} catch (IllegalArgumentException e) { 
			log.warn("Error saving inventory item: {}", e.getMessage());
			
			String errorMessage = e.getMessage();
			String toastMessage = null;

			if (errorMessage.startsWith("status.hasStock:") || errorMessage.startsWith("status.inRecipe:")) {
				String fieldSpecificError = errorMessage.substring(errorMessage.indexOf(':') + 1);
				result.addError(new FieldError("inventoryItemDto", "itemStatus", itemDto.getItemStatus(), false, null, null, fieldSpecificError));
				toastMessage = fieldSpecificError.replaceFirst("• ", "");
			} else if (errorMessage.contains("already exists")) {
				result.addError(new FieldError("inventoryItemDto", "name", itemDto.getName(), false, null, null,
						errorMessage));
				toastMessage = errorMessage;
			} else if (errorMessage.contains("threshold")) {
				result.reject("global", errorMessage); 
				toastMessage = errorMessage;
			} else {
				result.reject("global", errorMessage);
				toastMessage = "Error saving item: " + errorMessage;
			}
			
			redirectAttributes.addFlashAttribute("globalError", toastMessage);

			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryItemDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			addCommonAttributesForRedirect(redirectAttributes);
			
			String modal = "addItemModal";
			UriComponentsBuilder builder = uriBuilder.path("/admin/inventory").queryParam("showModal", modal);
			
			if (!isNew && itemDto.getId() != null) {
				builder = builder.queryParam("editId", itemDto.getId());
			}
			String redirectUrl = builder.build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/inventory";
	}

	@PostMapping("/stock/adjust")
	@PreAuthorize("hasAuthority('ADJUST_INVENTORY_STOCK')")
	public String adjustInventoryStock(
			@RequestParam("inventoryItemId") Long itemId,
			@RequestParam("quantity") BigDecimal quantity, 
			@RequestParam("action") String action,
			@RequestParam("reasonCategory") String reasonCategory, 
			@RequestParam(value = "reasonNote", required = false) String reasonNote, 
			RedirectAttributes redirectAttributes,
			Principal principal, UriComponentsBuilder uriBuilder) {

		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity must be a positive number.");
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		// Determine direction based on Action OR Reason
		// If reason is Expired/Damaged/Waste, force it to be a deduction
		boolean isWaste = List.of("Expired", "Damaged", "Waste").contains(reasonCategory);
		
		BigDecimal quantityChange;
		if (action.equals("deduct") || isWaste) {
			quantityChange = quantity.negate();
		} else {
			quantityChange = quantity;
		}

		// Construct formatted reason
		String finalReason = reasonCategory;
		if (StringUtils.hasText(reasonNote)) {
			finalReason += ": " + reasonNote;
		}

		try {
			InventoryItem updatedItem = inventoryItemService.adjustStock(itemId, quantityChange, finalReason);
			
			String actionText = (quantityChange.compareTo(BigDecimal.ZERO) > 0) ? "Increased" : "Deducted";
			String details = actionText + " " + quantity.abs() + " " + updatedItem.getUnit().getAbbreviation() + " of " + updatedItem.getName() + 
							 " (ID: " + itemId + "). Reason: " + finalReason;
			
			// Use specific log action prefix for waste so we can filter easily in the Waste Tab
			String logAction = isWaste ? "STOCK_WASTE_" + reasonCategory.toUpperCase() : "ADJUST_INVENTORY_STOCK";
			
			redirectAttributes.addFlashAttribute("stockSuccess", actionText + " stock for '" + updatedItem.getName()
					+ "' by " + quantity.abs() + ". New stock: " + updatedItem.getCurrentStock());
			
			activityLogService.logAdminAction(principal.getName(), logAction, details);
			
		} catch (RuntimeException e) { 
			log.error("Error adjusting inventory stock for item ID {}: {}", itemId, e.getMessage());
			redirectAttributes.addFlashAttribute("stockError", "Error adjusting stock: " + e.getMessage());
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/delete/{id}")
	@PreAuthorize("hasAuthority('DELETE_INVENTORY_ITEMS')")
	public String deleteOrDeactivateInventoryItem(@PathVariable("id") Long id,
			@RequestParam(value = "password", required = false) String password, RedirectAttributes redirectAttributes,
			Principal principal) {

		if (!adminService.validateOwnerPassword(password)) {
			redirectAttributes.addFlashAttribute("globalError", "Incorrect Owner Password. Action cancelled.");
			return "redirect:/admin/inventory";
		}

		Optional<InventoryItem> itemOpt = inventoryItemService.findById(id);
		if (itemOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Inventory item not found.");
			return "redirect:/admin/inventory";
		}

		InventoryItem item = itemOpt.get();
		String itemName = item.getName();

		if (item.getCurrentStock().compareTo(BigDecimal.ZERO) > 0) {
			log.warn("Admin {} attempted to delete/deactivate inventory item '{}' (ID: {}) with stock > 0. Blocked.",
					principal.getName(), itemName, id);
			redirectAttributes.addFlashAttribute("globalError", "Cannot delete or deactivate '" + itemName
					+ "'. Item still has " + item.getCurrentStock() + " in stock. Please adjust stock to 0 first.");
			return "redirect:/admin/inventory";
		}

		try {
			inventoryItemService.deleteItem(id);
			
			activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_ITEM",
					"Permanently deleted inventory item: " + itemName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess",
					"Item '" + itemName + "' deleted successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Failed to delete inventory item {}: {}", id, e.getMessage());
			
			String toastMessage = e.getMessage().replaceFirst("• ", "");
			redirectAttributes.addFlashAttribute("globalError", toastMessage);

		} catch (DataIntegrityViolationException e) {
			log.warn("Data integrity violation on delete for inventory item {}: {}", id, e.getMessage());
			redirectAttributes.addFlashAttribute("globalError", "Data integrity violation: Item is protected and cannot be deleted.");
		}

		return "redirect:/admin/inventory";
	}

	@PostMapping("/activate/{id}")
	@PreAuthorize("hasAuthority('DELETE_INVENTORY_ITEMS')")
	public String activateInventoryItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		try {
			Optional<InventoryItem> itemOpt = inventoryItemService.findById(id);
			if (itemOpt.isEmpty()) {
				redirectAttributes.addFlashAttribute("inventoryError", "Inventory item not found.");
				return "redirect:/admin/inventory";
			}
			String itemName = itemOpt.get().getName();

			inventoryItemService.activateItem(id);

			activityLogService.logAdminAction(principal.getName(), "ACTIVATE_INVENTORY_ITEM",
					"Activated inventory item: " + itemName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("inventorySuccess", "Item '" + itemName + "' activated successfully!");

		} catch (Exception e) {
			log.warn("Failed to activate inventory item {}: {}", id, e.getMessage());
			redirectAttributes.addFlashAttribute("globalError", e.getMessage());
		}

		return "redirect:/admin/inventory";
	}
}