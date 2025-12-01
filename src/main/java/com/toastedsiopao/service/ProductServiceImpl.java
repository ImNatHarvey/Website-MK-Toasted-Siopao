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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	private void validateNameUniqueness(String name, Long currentProductId) {
		Optional<Product> existingProductOpt = productRepository.findByNameIgnoreCase(name.trim());
		if (existingProductOpt.isPresent()) {
			Product existingProduct = existingProductOpt.get();
			if (currentProductId == null || !existingProduct.getId().equals(currentProductId)) {
				throw new IllegalArgumentException("Product name '" + name.trim() + "' already exists.");
			}
		}
	}

	private Page<Product> getPaginatedProducts(Page<Long> idPage, Pageable pageable) {
		List<Long> ids = idPage.getContent();
		if (ids.isEmpty()) {
			return new PageImpl<>(Collections.emptyList(), pageable, idPage.getTotalElements());
		}

		List<Product> products = productRepository.findWithDetailsByIds(ids);

		Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
		List<Product> sortedProducts = ids.stream().map(id -> productMap.get(id)).filter(p -> p != null)
				.collect(Collectors.toList());

		return new PageImpl<>(sortedProducts, pageable, idPage.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> findAll(Pageable pageable) {
		Page<Long> idPage = productRepository.findIdsAll(pageable);
		return getPaginatedProducts(idPage, pageable);
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
		if (!StringUtils.hasText(productDto.getName()))
			throw new IllegalArgumentException("Name required");
		if (productDto.getCategoryId() == null)
			throw new IllegalArgumentException("Category required");
		if (productDto.getPrice() == null)
			throw new IllegalArgumentException("Price required");

		validateThresholds(productDto.getLowStockThreshold(), productDto.getCriticalStockThreshold());
		validateNameUniqueness(productDto.getName(), productDto.getId());

		Category category = categoryRepository.findById(productDto.getCategoryId())
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + productDto.getCategoryId()));

		Product product;
		LocalDate anchorDateForExpiration = LocalDate.now();
		Integer oldExpirationDays = null;

		boolean isNew = productDto.getId() == null;

		if (!isNew) {
			product = productRepository.findById(productDto.getId())
					.orElseThrow(() -> new RuntimeException("Product not found with id: " + productDto.getId()));

			// Capture state before update for logic check
			oldExpirationDays = product.getExpirationDays();
			if (product.getStockLastUpdated() != null) {
				anchorDateForExpiration = product.getStockLastUpdated().toLocalDate();
			}

			if ("INACTIVE".equals(productDto.getProductStatus()) && "ACTIVE".equals(product.getProductStatus())) {
				if (product.getCurrentStock() > 0) {
					throw new IllegalArgumentException("status.hasStock:• Cannot deactivate '" + product.getName()
							+ "'. Product still has " + product.getCurrentStock() + " items in stock.");
				}
			}

		} else {
			product = new Product();
			product.setCurrentStock(0);
			product.setRecipeLocked(true);
		}

		product.setProductStatus(productDto.getProductStatus());

		if (!product.isRecipeLocked() || isNew) {
			if (product.getIngredients() != null) {
				List<Long> dtoIngredientItemIds = productDto.getIngredients().stream()
						.filter(dto -> dto.getInventoryItemId() != null).map(RecipeIngredientDto::getInventoryItemId)
						.collect(Collectors.toList());
				product.getIngredients()
						.removeIf(ingredient -> !dtoIngredientItemIds.contains(ingredient.getInventoryItem().getId()));
			}
			if (productDto.getIngredients() != null) {
				for (RecipeIngredientDto ingredientDto : productDto.getIngredients()) {
					if (ingredientDto.getInventoryItemId() != null && ingredientDto.getQuantityNeeded() != null
							&& ingredientDto.getQuantityNeeded().compareTo(BigDecimal.ZERO) > 0) {
						InventoryItem inventoryItem = inventoryItemRepository
								.findById(ingredientDto.getInventoryItemId())
								.orElseThrow(() -> new RuntimeException("Inventory Item not found"));
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

		// Update Expiration Days setting
		Integer newExpirationDays = productDto.getExpirationDays();
		product.setExpirationDays(newExpirationDays);

		// --- DATE LOGIC ---
		// 1. Update Created Date (Independent)
		if (productDto.getCreatedDate() != null) {
			product.setCreatedDate(productDto.getCreatedDate());
		} else if (isNew) {
			product.setCreatedDate(LocalDate.now());
		} else if (product.getCreatedDate() == null) {
			product.setCreatedDate(LocalDate.now());
		}

		// 2. Update Expiration Date
		// Anchor: Use 'Stock Last Updated' date, NOT 'Created Date'.
		boolean daysChanged = (newExpirationDays != null && !newExpirationDays.equals(oldExpirationDays))
				|| (newExpirationDays == null && oldExpirationDays != null);

		if (daysChanged || isNew) {
			if (newExpirationDays != null && newExpirationDays > 0) {
				product.setExpirationDate(anchorDateForExpiration.plusDays(newExpirationDays));
			} else {
				product.setExpirationDate(null);
			}
		}
		// --- END DATE LOGIC ---

		try {
			return productRepository.save(product);
		} catch (Exception e) {
			log.error("Error saving product: {}", e.getMessage(), e);
			throw new RuntimeException("Could not save product.", e);
		}
	}

	@Override
	public void deactivateProduct(Long id) {
		Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
		if (product.getCurrentStock() > 0)
			throw new IllegalArgumentException("Has stock");
		product.setProductStatus("INACTIVE");
		productRepository.save(product);
	}

	@Override
	public void activateProduct(Long id) {
		Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
		product.setProductStatus("ACTIVE");
		productRepository.save(product);
	}

	@Override
	public void deleteProduct(Long id) {
		Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
		if (product.getCurrentStock() > 0)
			throw new IllegalArgumentException("Has stock");
		if (orderItemRepository.countByProduct(product) > 0)
			throw new DataIntegrityViolationException("History");
		productRepository.delete(product);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> findByCategory(Long categoryId, Pageable pageable) {
		Category category = categoryRepository.findById(categoryId).orElseThrow();
		Page<Long> idPage = productRepository.findIdsByCategory(category, pageable);
		return getPaginatedProducts(idPage, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> searchProducts(String keyword, Pageable pageable) {
		if (keyword == null || keyword.trim().isEmpty()) {
			Page<Long> idPage = productRepository.findIdsAllActive(pageable);
			return getPaginatedProducts(idPage, pageable);
		}
		Page<Long> idPage = productRepository.findIdsActiveByNameContainingIgnoreCase(keyword.trim(), pageable);
		return getPaginatedProducts(idPage, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> searchProducts(String keyword, Long categoryId, Pageable pageable) {
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasCategory = categoryId != null;
		Page<Long> idPage;
		if (hasKeyword && hasCategory) {
			Category category = categoryRepository.findById(categoryId).orElseThrow();
			idPage = productRepository.findIdsActiveByNameContainingIgnoreCaseAndCategory(keyword.trim(), category,
					pageable);
		} else if (hasKeyword) {
			idPage = productRepository.findIdsActiveByNameContainingIgnoreCase(keyword.trim(), pageable);
		} else if (hasCategory) {
			Category category = categoryRepository.findById(categoryId).orElseThrow();
			idPage = productRepository.findIdsActiveByCategory(category, pageable);
		} else {
			idPage = productRepository.findIdsAllActive(pageable);
		}
		return getPaginatedProducts(idPage, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> searchAdminProducts(String keyword, Long categoryId, Pageable pageable) {
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasCategory = categoryId != null;
		Page<Long> idPage;
		if (hasKeyword && hasCategory) {
			Category category = categoryRepository.findById(categoryId).orElseThrow();
			idPage = productRepository.findIdsByNameContainingIgnoreCaseAndCategory(keyword.trim(), category, pageable);
		} else if (hasKeyword) {
			idPage = productRepository.findIdsByNameContainingIgnoreCase(keyword.trim(), pageable);
		} else if (hasCategory) {
			Category category = categoryRepository.findById(categoryId).orElseThrow();
			idPage = productRepository.findIdsByCategory(category, pageable);
		} else {
			idPage = productRepository.findIdsAll(pageable);
		}
		return getPaginatedProducts(idPage, pageable);
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
	public long countCriticalStockProducts() {
		return productRepository.countCriticalStockProducts();
	}

	@Override
	@Transactional(readOnly = true)
	public long countOutOfStockProducts() {
		return productRepository.countOutOfStockProducts();
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getProductMetrics(String keyword, Long categoryId) {
		Map<String, Object> metrics = new HashMap<>();

		Category category = null;
		if (categoryId != null) {
			category = categoryRepository.findById(categoryId).orElse(null);
		}
		String parsedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

		long totalProducts = productRepository.countFilteredProducts(parsedKeyword, category);
		long lowStock = productRepository.countFilteredLowStock(parsedKeyword, category);
		long criticalStock = productRepository.countFilteredCriticalStock(parsedKeyword, category);
		long outOfStock = productRepository.countFilteredOutOfStock(parsedKeyword, category);

		metrics.put("totalProducts", totalProducts);
		metrics.put("lowStock", lowStock);
		metrics.put("criticalStock", criticalStock);
		metrics.put("outOfStock", outOfStock);

		return metrics;
	}

	@Override
	@Transactional(readOnly = true)
	public int calculateMaxProducible(Long productId) {
		Optional<Product> productOpt = productRepository.findById(productId);
		if (productOpt.isEmpty() || productOpt.get().getIngredients().isEmpty())
			return 0;
		List<RecipeIngredient> ingredients = productOpt.get().getIngredients();
		int maxPossible = Integer.MAX_VALUE;
		for (RecipeIngredient ingredient : ingredients) {
			Optional<InventoryItem> itemOpt = inventoryItemService.findById(ingredient.getInventoryItem().getId());
			if (itemOpt.isEmpty())
				return 0;
			BigDecimal availableStock = itemOpt.get().getCurrentStock();
			BigDecimal quantityNeeded = ingredient.getQuantityNeeded();
			if (quantityNeeded == null || quantityNeeded.compareTo(BigDecimal.ZERO) <= 0)
				continue;
			if (availableStock.compareTo(quantityNeeded) < 0)
				return 0;
			int possibleUnits = availableStock.divide(quantityNeeded, 0, RoundingMode.FLOOR).intValue();
			if (possibleUnits < maxPossible)
				maxPossible = possibleUnits;
		}
		return (maxPossible == Integer.MAX_VALUE) ? 0 : maxPossible;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Product> findAllForReport(String keyword, Long categoryId) {
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasCategory = categoryId != null;
		if (hasKeyword && hasCategory) {
			Category category = categoryRepository.findById(categoryId).orElseThrow();
			return productRepository.findFullProductsByNameAndCategory(keyword.trim(), category);
		} else if (hasKeyword) {
			return productRepository.findFullProductsByName(keyword.trim());
		} else if (hasCategory) {
			Category category = categoryRepository.findById(categoryId).orElseThrow();
			return productRepository.findFullProductsByCategory(category);
		} else {
			return productRepository.findAllFullProducts();
		}
	}

	@Override
	public Product adjustStock(Long productId, int quantityChange, String reason, LocalDate createdDate,
			Integer expirationDays) {
		Product product = productRepository.findByIdForUpdate(productId)
				.orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

		if (quantityChange > 0 && "Production".equals(reason)) {
			List<RecipeIngredient> ingredients = product.getIngredients();
			if (ingredients != null && !ingredients.isEmpty()) {
				BigDecimal productionAmount = new BigDecimal(quantityChange);

				for (RecipeIngredient ingredient : ingredients) {
					InventoryItem item = ingredient.getInventoryItem();
					BigDecimal requiredQuantity = ingredient.getQuantityNeeded();
					if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0)
						continue;
					BigDecimal amountToDecrease = requiredQuantity.multiply(productionAmount);
					InventoryItem currentItemState = inventoryItemRepository.findByIdForUpdate(item.getId())
							.orElseThrow();
					if (currentItemState.getCurrentStock().compareTo(amountToDecrease) < 0) {
						throw new IllegalArgumentException("Insufficient inventory for '" + item.getName() + "'...");
					}
				}

				for (RecipeIngredient ingredient : ingredients) {
					InventoryItem item = ingredient.getInventoryItem();
					BigDecimal requiredQuantity = ingredient.getQuantityNeeded();
					if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0)
						continue;
					BigDecimal amountToDecrease = requiredQuantity.multiply(productionAmount);
					String deductionReason = "Production of " + quantityChange + "x " + product.getName();
					inventoryItemService.adjustStock(item.getId(), amountToDecrease.negate(), deductionReason);
				}
			}
		}

		if (quantityChange > 0) {
			// 1. Calculate new Expiration Date relative to TODAY (Date of stock
			// arrival/production)
			if (expirationDays != null && expirationDays > 0) {
				product.setExpirationDate(LocalDate.now().plusDays(expirationDays));
			} else {
				if (expirationDays != null)
					product.setExpirationDate(null);
			}

			// 2. Only update Created Date if explicitly provided (manual override),
			// otherwise keep original
			if (createdDate != null) {
				product.setCreatedDate(createdDate);
			}
		}

		int currentStock = product.getCurrentStock();
		int newStock = currentStock + quantityChange;
		if (newStock < 0)
			throw new IllegalArgumentException("Product stock cannot go below zero.");

		product.setCurrentStock(newStock);
		return productRepository.save(product);
	}
}