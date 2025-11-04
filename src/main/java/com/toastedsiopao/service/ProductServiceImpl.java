package com.toastedsiopao.service;

import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.dto.RecipeIngredientDto;
import com.toastedsiopao.model.*;
import com.toastedsiopao.repository.CategoryRepository;
import com.toastedsiopao.repository.InventoryItemRepository;
import com.toastedsiopao.repository.OrderItemRepository;
import com.toastedsiopao.repository.ProductRepository;
import com.toastedsiopao.service.InventoryItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Import StringUtils

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

	private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private InventoryItemRepository inventoryItemRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;
	@Autowired
	private InventoryItemService inventoryItemService;

	// --- Centralized Validation Method ---
	private void validateThresholds(Integer lowThreshold, Integer criticalThreshold) {
		if (criticalThreshold != null && lowThreshold != null && criticalThreshold > lowThreshold) {
			throw new IllegalArgumentException("Critical stock threshold cannot be greater than low stock threshold.");
		}
	}
	// --- End Validation Method ---

	@Override
	@Transactional(readOnly = true)
	public List<Product> findAll() {
		return productRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Product> findById(Long id) {
		return productRepository.findById(id);
	}

	@Override
	public Product save(ProductDto productDto) {
		// --- Input Validation ---
		if (productDto == null) {
			throw new IllegalArgumentException("Product data cannot be null.");
		}
		if (!StringUtils.hasText(productDto.getName())) {
			throw new IllegalArgumentException("Product name cannot be blank.");
		}
		if (productDto.getCategoryId() == null) {
			throw new IllegalArgumentException("Category must be selected.");
		}
		if (productDto.getPrice() == null) {
			throw new IllegalArgumentException("Price cannot be null.");
		}
		// Basic null checks for thresholds
		if (productDto.getLowStockThreshold() == null)
			productDto.setLowStockThreshold(0);
		if (productDto.getCriticalStockThreshold() == null)
			productDto.setCriticalStockThreshold(0);
		// --- End Input Validation ---

		// --- Moved Threshold Validation Here ---
		validateThresholds(productDto.getLowStockThreshold(), productDto.getCriticalStockThreshold());
		// --- End Moved Validation ---

		Category category = categoryRepository.findById(productDto.getCategoryId())
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + productDto.getCategoryId()));

		Product product;
		boolean isNew = productDto.getId() == null;
		String logAction = isNew ? "Creating" : "Updating";

		if (!isNew) {
			product = productRepository.findById(productDto.getId())
					.orElseThrow(() -> new RuntimeException("Product not found with id: " + productDto.getId()));
			log.info("{} product: ID={}, Name='{}'", logAction, product.getId(), productDto.getName());
		} else {
			product = new Product();
			product.setCurrentStock(0); // Set initial stock to 0 for new products
			log.info("{} new product: Name='{}'", logAction, productDto.getName());
			// Note: recipeLocked defaults to false
		}

		// --- MODIFIED: Ingredient Handling ---
		// Check if the recipe is locked. If it is, skip all ingredient modifications.
		if (product.isRecipeLocked()) {
			log.warn("Attempted to modify ingredients for locked product '{}' (ID: {}). Ingredients were not changed.",
					product.getName(), product.getId());
		} else {
			// Recipe is not locked, proceed with ingredient logic as normal.
			// Remove ingredients that are no longer in the DTO
			if (product.getIngredients() != null) { // Check if list exists (it should)
				List<Long> dtoIngredientItemIds = productDto.getIngredients().stream()
						.filter(dto -> dto.getInventoryItemId() != null) // Filter out null IDs
						.map(RecipeIngredientDto::getInventoryItemId).collect(Collectors.toList());

				product.getIngredients()
						.removeIf(ingredient -> !dtoIngredientItemIds.contains(ingredient.getInventoryItem().getId()));
			}

			// Update existing or add new
			if (productDto.getIngredients() != null) {
				for (RecipeIngredientDto ingredientDto : productDto.getIngredients()) {
					if (ingredientDto.getInventoryItemId() != null && ingredientDto.getQuantityNeeded() != null
							&& ingredientDto.getQuantityNeeded().compareTo(BigDecimal.ZERO) > 0) {

						InventoryItem inventoryItem = inventoryItemRepository
								.findById(ingredientDto.getInventoryItemId()).orElseThrow(() -> new RuntimeException(
										"Inventory Item not found with id: " + ingredientDto.getInventoryItemId()));

						// Check if this ingredient already exists and update it
						Optional<RecipeIngredient> existingIngredient = product.getIngredients().stream()
								.filter(ing -> ing.getInventoryItem() != null
										&& ing.getInventoryItem().getId().equals(ingredientDto.getInventoryItemId()))
								.findFirst();

						if (existingIngredient.isPresent()) {
							existingIngredient.get().setQuantityNeeded(ingredientDto.getQuantityNeeded());
						} else {
							// Add new one
							product.addIngredient(
									new RecipeIngredient(product, inventoryItem, ingredientDto.getQuantityNeeded()));
						}
					}
				}
			}
		}
		// --- End Ingredient Handling ---

		// Map basic fields
		product.setName(productDto.getName());
		product.setDescription(productDto.getDescription());
		product.setPrice(productDto.getPrice());
		product.setCategory(category);
		product.setImageUrl(productDto.getImageUrl());

		// Map thresholds (already validated)
		product.setLowStockThreshold(Optional.ofNullable(productDto.getLowStockThreshold()).orElse(0));
		product.setCriticalStockThreshold(Optional.ofNullable(productDto.getCriticalStockThreshold()).orElse(0));

		try {
			return productRepository.save(product);
		} catch (Exception e) {
			log.error("Database error {} product '{}': {}", logAction.toLowerCase(), productDto.getName(),
					e.getMessage(), e);
			throw new RuntimeException("Could not save product due to a database error.", e);
		}
	}

	@Override
	public void deleteById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

		// Check if product is in any order items
		List<OrderItem> orderItems = orderItemRepository.findByProduct(product);
		if (!orderItems.isEmpty()) {
			throw new RuntimeException("Cannot delete product '" + product.getName() + "'. It is part of "
					+ orderItems.size() + " existing order(s).");
		}

		log.info("Deleting product: ID={}, Name='{}'", id, product.getName());
		productRepository.deleteById(id);
	}

	// ... (find, search methods unchanged) ...
	@Override
	@Transactional(readOnly = true)
	public List<Product> findByCategory(Long categoryId) {
		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
		return productRepository.findByCategory(category);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Product> searchProducts(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return findAll();
		}
		return productRepository.findByNameContainingIgnoreCase(keyword.trim());
	}

	@Override // (Unchanged from original)
	public Product adjustStock(Long productId, int quantityChange, String reason) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

		// --- LOGIC FOR PRODUCTION (INCREASING PRODUCT STOCK) ---
		if (quantityChange > 0) {
			List<RecipeIngredient> ingredients = product.getIngredients();
			if (ingredients == null || ingredients.isEmpty()) {
				log.warn(
						"Product ID {} ('{}') has no ingredients defined. Increasing stock without consuming inventory.",
						productId, product.getName());
				// --- !! Even if no ingredients, we still lock the "empty" recipe !! ---
			} else {
				BigDecimal productionAmount = new BigDecimal(quantityChange);

				// 1. First, check if there is enough inventory for ALL ingredients
				for (RecipeIngredient ingredient : ingredients) {
					InventoryItem item = ingredient.getInventoryItem();
					if (item == null) {
						throw new RuntimeException("Recipe for product '" + product.getName()
								+ "' contains an invalid ingredient reference.");
					}
					BigDecimal requiredQuantity = ingredient.getQuantityNeeded();
					if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
						log.warn("Skipping ingredient '{}' for product '{}': quantity needed is zero or null.",
								item.getName(), product.getName());
						continue;
					}
					BigDecimal amountToDecrease = requiredQuantity.multiply(productionAmount);

					InventoryItem currentItemState = inventoryItemRepository.findById(item.getId())
							.orElseThrow(() -> new RuntimeException(
									"Inventory item '" + item.getName() + "' not found during stock check."));

					if (currentItemState.getCurrentStock().compareTo(amountToDecrease) < 0) {
						throw new IllegalArgumentException(
								"Insufficient inventory for '" + item.getName() + "'. Need " + amountToDecrease + " "
										+ item.getUnit().getAbbreviation() + " to produce " + quantityChange + " '"
										+ product.getName() + "', but only " + currentItemState.getCurrentStock() + " "
										+ item.getUnit().getAbbreviation() + " available.");
					}
				}

				// 2. If all checks pass, deduct inventory using InventoryItemService
				for (RecipeIngredient ingredient : ingredients) {
					InventoryItem item = ingredient.getInventoryItem();
					BigDecimal requiredQuantity = ingredient.getQuantityNeeded();
					if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
						continue;
					}

					BigDecimal amountToDecrease = requiredQuantity.multiply(productionAmount);
					String deductionReason = "Production of " + quantityChange + "x " + product.getName() + " (ID: "
							+ productId + ")";

					inventoryItemService.adjustStock(item.getId(), amountToDecrease.negate(), deductionReason);

					log.info("Deducted {} {} of '{}' from inventory for production.", amountToDecrease,
							item.getUnit().getAbbreviation(), item.getName());
				}
			}

			// --- NEW: SET RECIPE LOCK ---
			// If this is the first time stock is being added, lock the recipe.
			if (!product.isRecipeLocked()) {
				product.setRecipeLocked(true);
				log.info("Recipe for product '{}' (ID: {}) is now LOCKED due to first production.", product.getName(),
						productId);
			}
			// --- END NEW ---
		}
		// --- END PRODUCTION LOGIC ---

		// --- LOGIC FOR ALL ADJUSTMENTS (Update Product Stock) ---
		int currentStock = product.getCurrentStock();
		int newStock = currentStock + quantityChange;

		if (newStock < 0) {
			throw new IllegalArgumentException("Product stock cannot go below zero for '" + product.getName()
					+ "'. Current stock: " + currentStock + ", Change: " + quantityChange);
		}

		product.setCurrentStock(newStock);
		Product savedProduct = productRepository.save(product);

		log.info("Stock adjusted for Product ID {} ('{}'): Change={}, New Stock={}, Reason='{}'", productId,
				product.getName(), quantityChange, newStock, reason);

		return savedProduct;
	}
}