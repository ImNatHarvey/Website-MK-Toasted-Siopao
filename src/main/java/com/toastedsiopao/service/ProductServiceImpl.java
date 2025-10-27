package com.toastedsiopao.service;

import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.dto.RecipeIngredientDto;
import com.toastedsiopao.model.*;
import com.toastedsiopao.repository.CategoryRepository;
import com.toastedsiopao.repository.InventoryItemRepository;
import com.toastedsiopao.repository.OrderItemRepository; // This import will now work
import com.toastedsiopao.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
		Category category = categoryRepository.findById(productDto.getCategoryId())
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + productDto.getCategoryId()));

		Product product;
		boolean isNew = productDto.getId() == null;

		if (!isNew) {
			product = productRepository.findById(productDto.getId())
					.orElseThrow(() -> new RuntimeException("Product not found with id: " + productDto.getId()));

			// --- IMPROVED INGREDIENT HANDLING ---
			// Remove ingredients that are no longer in the DTO
			List<Long> dtoIngredientItemIds = productDto.getIngredients().stream()
					.map(RecipeIngredientDto::getInventoryItemId).collect(Collectors.toList());

			product.getIngredients()
					.removeIf(ingredient -> !dtoIngredientItemIds.contains(ingredient.getInventoryItem().getId()));

			// Update existing or add new
			for (RecipeIngredientDto ingredientDto : productDto.getIngredients()) {
				if (ingredientDto.getInventoryItemId() != null && ingredientDto.getQuantityNeeded() != null
						&& ingredientDto.getQuantityNeeded().compareTo(BigDecimal.ZERO) > 0) {

					InventoryItem inventoryItem = inventoryItemRepository.findById(ingredientDto.getInventoryItemId())
							.orElseThrow(() -> new RuntimeException(
									"Inventory Item not found with id: " + ingredientDto.getInventoryItemId()));

					// Check if this ingredient already exists and update it
					Optional<RecipeIngredient> existingIngredient = product.getIngredients().stream()
							.filter(ing -> ing.getInventoryItem().getId().equals(ingredientDto.getInventoryItemId()))
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
			// --- END IMPROVED HANDLING ---

		} else {
			product = new Product();
			product.setCurrentStock(0); // Set initial stock to 0 for new products

			// Process ingredients for new product
			if (productDto.getIngredients() != null) {
				for (RecipeIngredientDto ingredientDto : productDto.getIngredients()) {
					if (ingredientDto.getInventoryItemId() != null && ingredientDto.getQuantityNeeded() != null
							&& ingredientDto.getQuantityNeeded().compareTo(BigDecimal.ZERO) > 0) {
						InventoryItem inventoryItem = inventoryItemRepository
								.findById(ingredientDto.getInventoryItemId()).orElseThrow(() -> new RuntimeException(
										"Inventory Item not found with id: " + ingredientDto.getInventoryItemId()));
						RecipeIngredient recipeIngredient = new RecipeIngredient(product, inventoryItem,
								ingredientDto.getQuantityNeeded());
						product.addIngredient(recipeIngredient);
					}
				}
			}
		}

		// Map basic fields
		product.setName(productDto.getName());
		product.setDescription(productDto.getDescription());
		product.setPrice(productDto.getPrice());
		product.setCategory(category);
		product.setImageUrl(productDto.getImageUrl());

		if (productDto.getCriticalStockThreshold() > productDto.getLowStockThreshold()) {
			throw new IllegalArgumentException("Critical stock threshold cannot be greater than low stock threshold.");
		}
		product.setLowStockThreshold(productDto.getLowStockThreshold());
		product.setCriticalStockThreshold(productDto.getCriticalStockThreshold());

		return productRepository.save(product);
	}

	// --- Overridden deleteById method ---
	@Override
	public void deleteById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

		// Check if product is in any order items
		List<OrderItem> orderItems = orderItemRepository.findByProduct(product); // This line will now work
		if (!orderItems.isEmpty()) {
			throw new RuntimeException(
					"Cannot delete product. It is part of " + orderItems.size() + " existing order(s).");
		}

		productRepository.deleteById(id);
	}

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

	// --- HEAVILY MODIFIED: adjustStock method ---
	@Override
	public Product adjustStock(Long productId, int quantityChange, String reason) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

		// --- LOGIC FOR PRODUCTION (INCREASING STOCK) ---
		if (quantityChange > 0) {
			List<RecipeIngredient> ingredients = product.getIngredients();
			if (ingredients.isEmpty()) {
				log.warn("Product ID {} has no ingredients. Increasing stock without consuming inventory.", productId);
			}

			BigDecimal productionAmount = new BigDecimal(quantityChange);

			// 1. First, check if there is enough inventory for ALL ingredients
			for (RecipeIngredient ingredient : ingredients) {
				InventoryItem item = ingredient.getInventoryItem();
				BigDecimal amountToDecrease = ingredient.getQuantityNeeded().multiply(productionAmount);

				if (item.getCurrentStock().compareTo(amountToDecrease) < 0) {
					throw new IllegalArgumentException("Insufficient inventory for " + item.getName() + ". Need "
							+ amountToDecrease + " " + item.getUnit().getAbbreviation() + ", but only "
							+ item.getCurrentStock() + " " + item.getUnit().getAbbreviation() + " available.");
				}
			}

			// 2. If all checks pass, deduct inventory
			for (RecipeIngredient ingredient : ingredients) {
				InventoryItem item = ingredient.getInventoryItem();
				BigDecimal amountToDecrease = ingredient.getQuantityNeeded().multiply(productionAmount);

				item.setCurrentStock(item.getCurrentStock().subtract(amountToDecrease));
				inventoryItemRepository.save(item); // Save the updated inventory item

				log.info("Deducted {} {} from {} for production.", amountToDecrease, item.getUnit().getAbbreviation(),
						item.getName());
			}
		}
		// --- END PRODUCTION LOGIC ---

		// --- LOGIC FOR ALL ADJUSTMENTS (INCREASE OR DECREASE) ---
		int currentStock = product.getCurrentStock();
		int newStock = currentStock + quantityChange;

		if (newStock < 0) {
			throw new IllegalArgumentException(
					"Stock cannot go below zero. Current stock: " + currentStock + ", Change: " + quantityChange);
		}

		product.setCurrentStock(newStock);
		Product savedProduct = productRepository.save(product);

		log.info("Stock adjusted for Product ID {}: Change={}, New Stock={}, Reason={}", productId, quantityChange,
				newStock, reason);

		return savedProduct;
	}
}