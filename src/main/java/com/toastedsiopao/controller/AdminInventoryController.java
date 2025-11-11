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

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("inventoryCategories", inventoryCategoryService.findAll());
		redirectAttributes.addFlashAttribute("unitsOfMeasure", unitOfMeasureService.findAll());
		redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAll());
		redirectAttributes.addFlashAttribute("allInventoryItems", inventoryItemService.findAll());
	}

	@GetMapping
	@PreAuthorize("hasAuthority('VIEW_INVENTORY')")
	public String manageInventory(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<InventoryItem> inventoryPage = inventoryItemService.searchItems(keyword, categoryId, pageable);

		List<InventoryCategory> categories = inventoryCategoryService.findAll();
		List<UnitOfMeasure> units = unitOfMeasureService.findAll();
		List<InventoryItem> allItemsForStockModal = inventoryItemService.findAll();

		List<InventoryItem> allItems = inventoryItemService.findAll();
		BigDecimal totalInventoryValue = allItems.stream().map(InventoryItem::getTotalCostValue).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		List<InventoryItem> lowStockItems = inventoryItemService.findLowStockItems();
		List<InventoryItem> outOfStockItems = inventoryItemService.findOutOfStockItems();

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
		if (result.hasErrors()) {
			log.warn("Inventory item DTO validation failed. Errors: {}", result.getAllErrors());
			// --- MODIFIED: Add globalError for toast ---
			redirectAttributes.addFlashAttribute("globalError", "Validation failed. Please check the fields below.");
			// --- END MODIFICATION ---
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
		} catch (IllegalArgumentException e) { // Keep specific validation catch
			log.warn("Error saving inventory item: {}", e.getMessage());
			// --- MODIFIED: Add globalError for toast AND keep rejectValue ---
			if (e.getMessage().contains("already exists")) {
				result.addError(new FieldError("inventoryItemDto", "name", itemDto.getName(), false, null, null,
						e.getMessage()));
			} else if (e.getMessage().contains("threshold")) {
				result.reject("global", e.getMessage());
			}
			redirectAttributes.addFlashAttribute("globalError", "Error saving item: " + e.getMessage());
			// --- END MODIFICATION ---
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.inventoryItemDto",
					result);
			redirectAttributes.addFlashAttribute("inventoryItemDto", itemDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "addItemModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		// --- REMOVED: generic catch (RuntimeException e) block ---

		return "redirect:/admin/inventory";
	}

	@PostMapping("/stock/adjust")
	@PreAuthorize("hasAuthority('ADJUST_INVENTORY_STOCK')")
	public String adjustInventoryStock(@RequestParam("inventoryItemId") Long itemId,
			@RequestParam("quantity") BigDecimal quantity, @RequestParam("action") String action,
			@RequestParam(value = "reason", required = false) String reason, RedirectAttributes redirectAttributes,
			Principal principal, UriComponentsBuilder uriBuilder) {

		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity must be a positive number.");
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

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
		} catch (RuntimeException e) { // Keep this for stock-specific errors like "cannot go below zero"
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
	public String deleteInventoryItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		// --- REMOVED: try-catch block ---

		Optional<InventoryItem> itemOpt = inventoryItemService.findById(id);
		if (itemOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("inventoryError", "Inventory item not found.");
			return "redirect:/admin/inventory";
		}
		String itemName = itemOpt.get().getName();

		// Let the service throw an exception if deletion fails
		// The GlobalExceptionHandler will catch it.
		inventoryItemService.deleteById(id);

		activityLogService.logAdminAction(principal.getName(), "DELETE_INVENTORY_ITEM",
				"Deleted inventory item: " + itemName + " (ID: " + id + ")");
		redirectAttributes.addFlashAttribute("inventorySuccess", "Item '" + itemName + "' deleted successfully!");

		return "redirect:/admin/inventory";
	}
}