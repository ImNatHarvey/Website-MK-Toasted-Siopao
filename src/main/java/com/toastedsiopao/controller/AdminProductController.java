package com.toastedsiopao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toastedsiopao.dto.CategoryDto;
import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.repository.OrderItemRepository;
import com.toastedsiopao.repository.ProductRepository;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.AdminService;
import com.toastedsiopao.service.CategoryService;
import com.toastedsiopao.service.FileStorageService;
import com.toastedsiopao.service.InventoryItemService;
import com.toastedsiopao.service.ProductService;
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
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

	private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

	@Autowired
	private ProductService productService;
	@Autowired
	private CategoryService categoryService;
	@Autowired
	private InventoryItemService inventoryItemService;
	@Autowired
	private ActivityLogService activityLogService;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private AdminService adminService;

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("categories", categoryService.findAll());
		redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAllActive());
	}

	@GetMapping
	@PreAuthorize("hasAuthority('VIEW_PRODUCTS')")
	public String manageProducts(Model model, @RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "8") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<Product> productPage;

		List<Category> categoryList = categoryService.findAll();
		List<InventoryItem> inventoryItems = inventoryItemService.findAllActive();

		log.info("Fetching products with keyword: '{}', categoryId: {}, page: {}, size: {}", keyword, categoryId, page,
				size);
		productPage = productService.searchAdminProducts(keyword, categoryId, pageable);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategoryId", categoryId);

		Map<String, Object> metrics = productService.getProductMetrics(keyword, categoryId);

		model.addAttribute("totalProducts", metrics.get("totalProducts"));
		model.addAttribute("lowStockProducts", metrics.get("lowStock"));
		model.addAttribute("outOfStockProducts", metrics.get("outOfStock"));
		model.addAttribute("criticalStockProducts", metrics.get("criticalStock"));

		model.addAttribute("productPage", productPage);
		model.addAttribute("products", productPage.getContent());
		model.addAttribute("categories", categoryList);
		model.addAttribute("inventoryItems", inventoryItems);

		Page<Product> allProductsPage = productService.searchProducts(null, null, Pageable.unpaged());
		model.addAttribute("allProductsForStockModal", allProductsPage.getContent());

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", productPage.getTotalPages());
		model.addAttribute("totalItems", productPage.getTotalElements());
		model.addAttribute("size", size);

		if (!model.containsAttribute("productDto")) {
			model.addAttribute("productDto", new ProductDto());
		}
		if (!model.containsAttribute("categoryDto")) {
			model.addAttribute("categoryDto", new CategoryDto());
		}
		if (!model.containsAttribute("productUpdateDto")) {
			model.addAttribute("productUpdateDto", new ProductDto());
		}
		if (!model.containsAttribute("categoryUpdateDto")) {
			model.addAttribute("categoryUpdateDto", new CategoryDto());
		}
		return "admin/products";
	}

	@PostMapping("/add")
	@PreAuthorize("hasAuthority('ADD_PRODUCTS')")
	public String addProduct(@Valid @ModelAttribute("productDto") ProductDto productDto, BindingResult result,
			@RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			log.warn("Product DTO validation failed for add. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("globalError", "Validation failed. Please check the fields below.");
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", result);
			redirectAttributes.addFlashAttribute("productDto", productDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "addProductModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			if (imageFile != null && !imageFile.isEmpty()) {
				try {
					String imagePath = fileStorageService.store(imageFile);
					productDto.setImageUrl(imagePath);
				} catch (Exception e) {
					log.error("Error storing image file during add: {}", e.getMessage());
					result.reject("global", "Could not save image: " + e.getMessage());
					redirectAttributes.addFlashAttribute("globalError", "Error adding product: " + e.getMessage());
					redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto",
							result);
					redirectAttributes.addFlashAttribute("productDto", productDto);
					addCommonAttributesForRedirect(redirectAttributes);
					String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "addProductModal")
							.build().toUriString();
					return "redirect:" + redirectUrl;
				}
			}

			Product savedProduct = productService.save(productDto);
			activityLogService.logAdminAction(principal.getName(), "ADD_PRODUCT",
					"Added new product: " + savedProduct.getName() + " (ID: " + savedProduct.getId() + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + savedProduct.getName() + "' added successfully!");

		} catch (RuntimeException e) {
			log.warn("Error adding product: {}", e.getMessage(), e);

			String errorMessage = e.getMessage();
			String toastMessage = null;

			if (errorMessage.contains("Product name")) {
				result.rejectValue("name", "productDto.name", "• " + errorMessage);
				toastMessage = errorMessage;
			} else if (errorMessage.contains("threshold")) {
				result.reject("global", "• " + errorMessage);
				toastMessage = errorMessage;
			} else {
				result.reject("global", "• Error adding product: " + errorMessage);
				toastMessage = "Error adding product: " + errorMessage;
			}

			redirectAttributes.addFlashAttribute("globalError", toastMessage);
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", result);
			redirectAttributes.addFlashAttribute("productDto", productDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "addProductModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/products";
	}

	@PostMapping("/update")
	@PreAuthorize("hasAuthority('EDIT_PRODUCTS')")
	public String updateProduct(@Valid @ModelAttribute("productUpdateDto") ProductDto productDto, BindingResult result,
			@RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			log.warn("Product DTO validation failed for update. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("globalError", "Validation failed. Please check the fields below.");
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("productUpdateDto", productDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "editProductModal")
					.queryParam("editId", productDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			if (productDto.getId() == null)
				throw new IllegalArgumentException("Product ID is missing for update.");

			String oldImagePath = productDto.getImageUrl();

			if (productDto.isRemoveImage()) {
				log.info("User requested image removal for product ID: {}", productDto.getId());
				if (StringUtils.hasText(oldImagePath)) {
					fileStorageService.delete(oldImagePath);
				}
				productDto.setImageUrl(null);
			} else if (imageFile != null && !imageFile.isEmpty()) {
				log.info("User uploaded new image for product ID: {}", productDto.getId());
				try {
					String newImagePath = fileStorageService.store(imageFile);
					productDto.setImageUrl(newImagePath);
					if (StringUtils.hasText(oldImagePath)) {
						fileStorageService.delete(oldImagePath);
					}
				} catch (Exception e) {
					log.error("Error storing new image file during update: {}", e.getMessage());
					result.reject("global", "• Could not save new image: " + e.getMessage());
					redirectAttributes.addFlashAttribute("globalError", "Error updating product: " + e.getMessage());
					redirectAttributes
							.addFlashAttribute("org.springframework.validation.BindingResult.productUpdateDto", result);
					redirectAttributes.addFlashAttribute("productUpdateDto", productDto);
					addCommonAttributesForRedirect(redirectAttributes);
					String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "editProductModal")
							.queryParam("editId", productDto.getId()).build().toUriString();
					return "redirect:" + redirectUrl;
				}
			} else {
				log.debug("No image change for product ID: {}", productDto.getId());
			}

			Product savedProduct = productService.save(productDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_PRODUCT",
					"Updated product: " + savedProduct.getName() + " (ID: " + savedProduct.getId() + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + savedProduct.getName() + "' updated successfully!");

		} catch (RuntimeException e) {
			log.warn("Error updating product: {}", e.getMessage(), e);

			String errorMessage = e.getMessage();
			String toastMessage = null;

			if (errorMessage.startsWith("status.hasStock:")) {
				String fieldSpecificError = errorMessage.substring(errorMessage.indexOf(':') + 1);
				result.addError(new FieldError("productUpdateDto", "productStatus", productDto.getProductStatus(),
						false, null, null, fieldSpecificError));
				toastMessage = fieldSpecificError.replaceFirst("• ", "");
			} else if (errorMessage.contains("Product name")) {
				result.rejectValue("name", "productUpdateDto.name", "• " + errorMessage);
				toastMessage = errorMessage;
			} else if (errorMessage.contains("threshold")) {
				result.reject("global", "• " + errorMessage);
				toastMessage = errorMessage;
			} else {
				result.reject("global", "• Error updating product: " + errorMessage);
				toastMessage = "Error updating product: " + errorMessage;
			}

			redirectAttributes.addFlashAttribute("globalError", toastMessage);
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("productUpdateDto", productDto);
			addCommonAttributesForRedirect(redirectAttributes);
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "editProductModal")
					.queryParam("editId", productDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/products";
	}

	@PostMapping("/stock/adjust")
	@PreAuthorize("hasAuthority('ADJUST_PRODUCT_STOCK')")
	public String adjustProductStock(@RequestParam("productId") Long productId, @RequestParam("quantity") int quantity,
			@RequestParam("action") String action,
			@RequestParam(value = "reasonCategory", required = false) String reasonCategory,
			@RequestParam(value = "reasonNote", required = false) String reasonNote,
			@RequestParam(value = "receivedDate", required = false) LocalDate receivedDate,
			@RequestParam(value = "expirationDays", required = false) Integer expirationDays,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (quantity <= 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity must be a positive number.");
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		int quantityChange = action.equals("deduct") ? -quantity : quantity;
		String derivedReason = action.equals("add") ? "Production" : "Adjustment/Wastage";

		boolean isWasteAction = action.equals("deduct")
				&& List.of("Expired", "Damaged", "Waste").contains(reasonCategory);

		String finalReason = derivedReason;
		if (StringUtils.hasText(reasonCategory)) {
			finalReason = reasonCategory;
		}
		if (StringUtils.hasText(reasonNote)) {
			finalReason += ": " + reasonNote;
		}

		try {
			// --- FIX: Pass receivedDate (likely null from modal) directly ---
			Product updatedProduct = productService.adjustStock(productId, quantityChange, finalReason, receivedDate,
					expirationDays);

			String actionText = action.equals("add") ? "Added" : "Deducted";
			if ("Production".equals(reasonCategory))
				actionText = "Produced";
			else if (isWasteAction)
				actionText = "Wasted";

			String details = actionText + " " + quantity + " unit(s) of " + updatedProduct.getName() + " (ID: "
					+ productId + "). Reason: " + finalReason;

			if (isWasteAction) {
				BigDecimal unitValue = updatedProduct.getPrice();
				BigDecimal quantityDecimal = BigDecimal.valueOf(quantity);

				String logAction = "PRODUCT_WASTE_" + reasonCategory.toUpperCase();
				activityLogService.logWasteAction(principal.getName(), logAction, details, updatedProduct.getName(),
						quantityDecimal, unitValue);
			} else {
				activityLogService.logAdminAction(principal.getName(), "ADJUST_PRODUCT_STOCK", details);
			}

			redirectAttributes.addFlashAttribute("stockSuccess", actionText + " " + quantity + " unit(s) of '"
					+ updatedProduct.getName() + "'. New stock: " + updatedProduct.getCurrentStock());

		} catch (RuntimeException e) {
			log.warn("Stock adjustment failed: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("stockError", "Error adjusting stock: " + e.getMessage());
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		return "redirect:/admin/products";
	}

	@PostMapping("/delete/{id}")
	@PreAuthorize("hasAuthority('DELETE_PRODUCTS')")
	public String deleteOrDeactivateProduct(@PathVariable("id") Long id,
			@RequestParam(value = "password", required = false) String password, RedirectAttributes redirectAttributes,
			Principal principal) {

		if (!adminService.validateOwnerPassword(password)) {
			redirectAttributes.addFlashAttribute("globalError", "Incorrect Owner Password. Action cancelled.");
			return "redirect:/admin/products";
		}

		Optional<Product> productOpt = productService.findById(id);
		if (productOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("productError", "Product not found.");
			return "redirect:/admin/products";
		}

		Product product = productOpt.get();
		String productName = product.getName();

		if (product.getCurrentStock() > 0) {
			log.warn("Admin {} attempted to delete/deactivate product '{}' (ID: {}) with stock > 0. Blocked.",
					principal.getName(), productName, id);
			redirectAttributes.addFlashAttribute("globalError",
					"Cannot delete or deactivate '" + productName + "'. Product still has " + product.getCurrentStock()
							+ " items in stock. Please adjust stock to 0 first.");
			return "redirect:/admin/products";
		}

		try {
			if (orderItemRepository.countByProduct(product) > 0) {
				productService.deactivateProduct(id);
				activityLogService.logAdminAction(principal.getName(), "DEACTIVATE_PRODUCT",
						"Deactivated product with order history: " + productName + " (ID: " + id + ")");
				redirectAttributes.addFlashAttribute("productSuccess",
						"Product '" + productName + "' has order history. It has been DEACTIVATED instead of deleted.");
			} else {
				String imagePath = product.getImageUrl();
				if (StringUtils.hasText(imagePath)) {
					fileStorageService.delete(imagePath);
				}
				productService.deleteProduct(id);
				activityLogService.logAdminAction(principal.getName(), "DELETE_PRODUCT",
						"Permanently deleted product: " + productName + " (ID: " + id + ")");
				redirectAttributes.addFlashAttribute("productSuccess",
						"Product '" + productName + "' had no order history and was PERMANENTLY deleted.");
			}
		} catch (DataIntegrityViolationException e) {
			log.warn("Data integrity violation on delete/deactivate for product {}: {}", id, e.getMessage());
			redirectAttributes.addFlashAttribute("globalError",
					"Operation failed. Product has order history and cannot be deleted.");
		} catch (IllegalArgumentException e) {
			log.warn("Failed to delete/deactivate product {}: {}", id, e.getMessage());
			redirectAttributes.addFlashAttribute("globalError", e.getMessage());
		}

		return "redirect:/admin/products";
	}

	@PostMapping("/activate/{id}")
	@PreAuthorize("hasAuthority('DELETE_PRODUCTS')")
	public String activateProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {

		try {
			Optional<Product> productOpt = productService.findById(id);
			if (productOpt.isEmpty()) {
				redirectAttributes.addFlashAttribute("productError", "Product not found.");
				return "redirect:/admin/products";
			}
			String productName = productOpt.get().getName();

			productService.activateProduct(id);

			activityLogService.logAdminAction(principal.getName(), "ACTIVATE_PRODUCT",
					"Activated product: " + productName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + productName + "' activated successfully!");

		} catch (Exception e) {
			log.warn("Failed to activate product {}: {}", id, e.getMessage());
			redirectAttributes.addFlashAttribute("globalError", e.getMessage());
		}

		return "redirect:/admin/products";
	}

	@GetMapping("/calculate-max/{id}")
	@ResponseBody
	@PreAuthorize("hasAuthority('ADJUST_PRODUCT_STOCK')")
	public Map<String, Integer> getCalculatedMax(@PathVariable("id") Long id) {
		try {
			int max = productService.calculateMaxProducible(id);
			return Map.of("maxQuantity", max);
		} catch (Exception e) {
			log.error("Error calculating max producible for product ID {}: {}", id, e.getMessage());
			return Map.of("maxQuantity", 0);
		}
	}
}