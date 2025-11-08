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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

	private void validateThresholds(Integer lowThreshold, Integer criticalThreshold) {
		
		if (lowThreshold == null || lowThreshold <= 0) {
			throw new IllegalArgumentException("Low stock threshold must be greater than 0.");
		}
		if (criticalThreshold == null || criticalThreshold <= 0) {
			throw new IllegalArgumentException("Critical stock threshold must be greater than 0.");
		}
		if (criticalThreshold > lowThreshold) {
			throw new IllegalArgumentException("Critical stock threshold cannot be greater than low stock threshold.");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> findAll(Pageable pageable) { 
		return productRepository.findAll(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Product> findById(Long id) {
		return productRepository.findById(id);
	}

	@Override
	public Product save(ProductDto productDto) {
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
		
		validateThresholds(productDto.getLowStockThreshold(), productDto.getCriticalStockThreshold());

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
			product.setCurrentStock(0); 
			log.info("{} new product: Name='{}'", logAction, productDto.getName());
			product.setRecipeLocked(true); 
			log.info("Setting recipeLocked=true for new product '{}'", productDto.getName());
		}

		if (product.isRecipeLocked()) {
			if (!isNew) {
				log.warn(
						"Attempted to modify ingredients for locked product '{}' (ID: {}). Ingredients were not changed.",
						product.getName(), product.getId());
			}
		}

		if (!product.isRecipeLocked() || isNew) {
			if (product.getIngredients() != null) { 
				List<Long> dtoIngredientItemIds = productDto.getIngredients().stream()
						.filter(dto -> dto.getInventoryItemId() != null) 
						.map(RecipeIngredientDto::getInventoryItemId).collect(Collectors.toList());

				product.getIngredients()
						.removeIf(ingredient -> !dtoIngredientItemIds.contains(ingredient.getInventoryItem().getId()));
			}

			if (productDto.getIngredients() != null) {
				for (RecipeIngredientDto ingredientDto : productDto.getIngredients()) {
					if (ingredientDto.getInventoryItemId() != null && ingredientDto.getQuantityNeeded() != null
							&& ingredientDto.getQuantityNeeded().compareTo(BigDecimal.ZERO) > 0) {

						InventoryItem inventoryItem = inventoryItemRepository
								.findById(ingredientDto.getInventoryItemId()).orElseThrow(() -> new RuntimeException(
										"Inventory Item not found with id: " + ingredientDto.getInventoryItemId()));

						Optional<RecipeIngredient> existingIngredient = product.getIngredients().stream()
								.filter(ing -> ing.getInventoryItem() != null
										&& ing.getInventoryItem().getId().equals(ingredientDto.getInventoryItemId()))
								.findFirst();

						if (existingIngredient.isPresent()) {
							existingIngredient.get().setQuantityNeeded(ingredientDto.getQuantityNeeded());
						} else {
							product.addIngredient(
									new RecipeIngredient(product, inventoryItem, ingredientDto.getQuantityNeeded()));
						}
					}
				}
			}
		}

		product.setName(productDto.getName());
		product.setDescription(productDto.getDescription());
		product.setPrice(productDto.getPrice());
		product.setCategory(category);
		product.setImageUrl(productDto.getImageUrl());

		product.setLowStockThreshold(productDto.getLowStockThreshold());
		product.setCriticalStockThreshold(productDto.getCriticalStockThreshold());

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

		List<OrderItem> orderItems = orderItemRepository.findByProduct(product);
		if (!orderItems.isEmpty()) {
			throw new RuntimeException("Cannot delete product '" + product.getName() + "'. It is part of "
					+ orderItems.size() + " existing order(s).");
		}

		log.info("Deleting product: ID={}, Name='{}'", id, product.getName());
		productRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> findByCategory(Long categoryId, Pageable pageable) {
		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
		return productRepository.findByCategory(category, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> searchProducts(String keyword, Pageable pageable) { 
		if (keyword == null || keyword.trim().isEmpty()) {
			return findAll(pageable);
		}
		return productRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> searchProducts(String keyword, Long categoryId, Pageable pageable) {
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasCategory = categoryId != null;

		if (hasKeyword && hasCategory) {
			Category category = categoryRepository.findById(categoryId)
					.orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
			return productRepository.findByNameContainingIgnoreCaseAndCategory(keyword.trim(), category, pageable);
		} else if (hasKeyword) {
			return productRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
		} else if (hasCategory) {
			Category category = categoryRepository.findById(categoryId)
					.orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
			return productRepository.findByCategory(category, pageable);
		} else {
			return productRepository.findAll(pageable);
		}
	}

	@Override 
	public Product adjustStock(Long productId, int quantityChange, String reason) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

		if (quantityChange > 0) {
			List<RecipeIngredient> ingredients = product.getIngredients();
			if (ingredients == null || ingredients.isEmpty()) {
				log.warn(
						"Product ID {} ('{}') has no ingredients defined. Increasing stock without consuming inventory.",
						productId, product.getName());
			} else {
				BigDecimal productionAmount = new BigDecimal(quantityChange);

				// --- RACE CONDITION FIX: START ---
				// This loop now CHECKS and LOCKS inventory items.
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

					// Find and lock the inventory item row for this transaction.
					// This call will block if another transaction has locked the same row.
					InventoryItem currentItemState = inventoryItemRepository.findByIdForUpdate(item.getId())
							.orElseThrow(() -> new RuntimeException(
									"Inventory item '" + item.getName() + "' not found and could not be locked."));

					if (currentItemState.getCurrentStock().compareTo(amountToDecrease) < 0) {
						// Because we have a lock, this check is definitive for this transaction.
						// The transaction will roll back, releasing the lock.
						throw new IllegalArgumentException(
								"Insufficient inventory for '" + item.getName() + "'. Need " + amountToDecrease + " "
										+ item.getUnit().getAbbreviation() + " to produce " + quantityChange + " '"
										+ product.getName() + "', but only " + currentItemState.getCurrentStock() + " "
										+ item.getUnit().getAbbreviation() + " available.");
					}
				}
				// --- RACE CONDITION FIX: END ---

				// This second loop performs the deduction.
				// It will re-use the entities already locked and loaded into the
				// persistence context by the loop above.
				for (RecipeIngredient ingredient : ingredients) {
					InventoryItem item = ingredient.getInventoryItem();
					BigDecimal requiredQuantity = ingredient.getQuantityNeeded();
					if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
						continue;
					}

					BigDecimal amountToDecrease = requiredQuantity.multiply(productionAmount);
					String deductionReason = "Production of " + quantityChange + "x " + product.getName() + " (ID: "
							+ productId + ")";

					// This service call will find the already-locked item and update it.
					inventoryItemService.adjustStock(item.getId(), amountToDecrease.negate(), deductionReason);

					log.info("Deducted {} {} of '{}' from inventory for production.", amountToDecrease,
							item.getUnit().getAbbreviation(), item.getName());
				}
			}
		}
		
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
	
	@Override
	@Transactional(readOnly = true)
	public long countAllProducts() {
		return productRepository.count();
	}

	@Override
	@Transactional(readOnly = true)
	public long countLowStockProducts() {
		return productRepository.countLowStockProducts();
	}

	@Override
	@Transactional(readOnly = true)
	public long countOutOfStockProducts() {
		return productRepository.countOutOfStockProducts();
	}

	@Override
	@Transactional(readOnly = true)
	public int calculateMaxProducible(Long productId) {
		Optional<Product> productOpt = productRepository.findById(productId);
		if (productOpt.isEmpty()) {
			log.warn("calculateMaxProducible: Product not found with ID {}", productId);
			return 0;
		}

		List<RecipeIngredient> ingredients = productOpt.get().getIngredients();
		if (ingredients == null || ingredients.isEmpty()) {
			log.warn("calculateMaxProducible: Product '{}' has no ingredients.", productOpt.get().getName());
			return 0;
		}

		int maxPossible = Integer.MAX_VALUE;

		for (RecipeIngredient ingredient : ingredients) {
			Optional<InventoryItem> itemOpt = inventoryItemService.findById(ingredient.getInventoryItem().getId());

			if (itemOpt.isEmpty()) {
				log.warn("calculateMaxProducible: Ingredient item ID {} not found.",
						ingredient.getInventoryItem().getId());
				return 0; 
			}

			BigDecimal availableStock = itemOpt.get().getCurrentStock();
			BigDecimal quantityNeeded = ingredient.getQuantityNeeded();

			if (quantityNeeded == null || quantityNeeded.compareTo(BigDecimal.ZERO) <= 0) {
				log.warn("calculateMaxProducible: Ingredient '{}' has invalid quantity needed ({}).",
						itemOpt.get().getName(), quantityNeeded);
				continue; 
			}

			if (availableStock.compareTo(quantityNeeded) < 0) {
				log.debug("calculateMaxProducible: Not enough stock for '{}'. Need {}, have {}.",
						itemOpt.get().getName(), quantityNeeded, availableStock);
				return 0; 
			}

			int possibleUnits = availableStock.divide(quantityNeeded, 0, RoundingMode.FLOOR).intValue();

			if (possibleUnits < maxPossible) {
				maxPossible = possibleUnits;
			}
		}

		return (maxPossible == Integer.MAX_VALUE) ? 0 : maxPossible;
	}
}