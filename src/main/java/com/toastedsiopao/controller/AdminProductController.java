package com.toastedsiopao.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toastedsiopao.dto.CategoryDto;
import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.CategoryService;
import com.toastedsiopao.service.FileStorageService;
import com.toastedsiopao.service.InventoryItemService;
import com.toastedsiopao.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
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
	private FileStorageService fileStorageService;

	private void addCommonAttributesForRedirect(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("categories", categoryService.findAll());
		redirectAttributes.addFlashAttribute("inventoryItems", inventoryItemService.findAll());
	}

	@GetMapping
	public String manageProducts(Model model, @RequestParam(value = "category", required = false) Long categoryId,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "8") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<Product> productPage;

		List<Category> categoryList = categoryService.findAll();
		List<InventoryItem> inventoryItems = inventoryItemService.findAll();

		log.info("Fetching products with keyword: '{}', categoryId: {}, page: {}, size: {}", keyword, categoryId, page,
				size);
		productPage = productService.searchProducts(keyword, categoryId, pageable);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategoryId", categoryId);

		long totalProducts = productService.countAllProducts();
		long lowStockProducts = productService.countLowStockProducts();
		long outOfStockProducts = productService.countOutOfStockProducts();

		model.addAttribute("totalProducts", totalProducts);
		model.addAttribute("lowStockProducts", lowStockProducts);
		model.addAttribute("outOfStockProducts", outOfStockProducts);

		model.addAttribute("productPage", productPage);
		model.addAttribute("products", productPage.getContent());
		model.addAttribute("categories", categoryList);
		model.addAttribute("inventoryItems", inventoryItems);

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", productPage.getTotalPages());
		model.addAttribute("totalItems", productPage.getTotalElements());
		model.addAttribute("size", size);

		Map<Long, BigDecimal> inventoryStockMap = new HashMap<>();
		for (InventoryItem item : inventoryItems) {
			inventoryStockMap.put(item.getId(), item.getCurrentStock());
		}
		String inventoryStockMapJson = "{}";
		try {
			inventoryStockMapJson = objectMapper.writeValueAsString(inventoryStockMap);
		} catch (JsonProcessingException e) {
			log.error("Error converting inventory stock map to JSON", e);
		}
		model.addAttribute("inventoryStockMapJson", inventoryStockMapJson);
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
	public String addProduct(@Valid @ModelAttribute("productDto") ProductDto productDto, BindingResult result,
			@RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			log.warn("Product DTO validation failed for add. Errors: {}", result.getAllErrors());
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
			if (e.getMessage().contains("threshold")) {
				result.reject("global", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("productError", "Error adding product: " + e.getMessage());
			}
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
	public String updateProduct(@Valid @ModelAttribute("productUpdateDto") ProductDto productDto, BindingResult result,
			@RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
			RedirectAttributes redirectAttributes, Principal principal, UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			log.warn("Product DTO validation failed for update. Errors: {}", result.getAllErrors());
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
					result.reject("global", "Could not save new image: " + e.getMessage());
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
			if (e.getMessage().contains("threshold")) {
				result.reject("global", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("productError", "Error updating product: " + e.getMessage());
			}
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

	// All /categories/* POST mappings removed

	@PostMapping("/stock/adjust")
	public String adjustProductStock(@RequestParam("productId") Long productId, @RequestParam("quantity") int quantity,
			@RequestParam("action") String action, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		if (quantity <= 0) {
			redirectAttributes.addFlashAttribute("stockError", "Quantity must be a positive number.");
			String redirectUrl = uriBuilder.path("/admin/products").queryParam("showModal", "manageStockModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		int quantityChange = action.equals("deduct") ? -quantity : quantity;
		String derivedReason = action.equals("add") ? "Production" : "Adjustment/Wastage";

		try {
			Product updatedProduct = productService.adjustStock(productId, quantityChange, derivedReason);
			String actionText = action.equals("add") ? "Produced" : "Adjusted";
			String details = actionText + " " + quantity + " units of " + updatedProduct.getName() + " (ID: "
					+ productId + "). Reason: " + derivedReason;
			redirectAttributes.addFlashAttribute("stockSuccess", actionText + " " + quantity + " units of '"
					+ updatedProduct.getName() + "'. New stock: " + updatedProduct.getCurrentStock());
			activityLogService.logAdminAction(principal.getName(), "ADJUST_PRODUCT_STOCK", details);
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
	public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
			Principal principal) {
		Optional<Product> productOpt = productService.findById(id);
		if (productOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("productError", "Product not found.");
			return "redirect:/admin/products";
		}
		String productName = productOpt.get().getName();
		try {
			String imagePath = productOpt.get().getImageUrl();
			if (StringUtils.hasText(imagePath)) {
				fileStorageService.delete(imagePath);
			}

			productService.deleteById(id);
			activityLogService.logAdminAction(principal.getName(), "DELETE_PRODUCT",
					"Deleted product: " + productName + " (ID: " + id + ")");
			redirectAttributes.addFlashAttribute("productSuccess",
					"Product '" + productName + "' deleted successfully!");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("productError",
					"Could not delete product '" + productName + "': " + e.getMessage());
		}
		return "redirect:/admin/products";
	}
}