package com.toastedsiopao.service;

import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.model.Product;
import java.math.BigDecimal; // Import BigDecimal
import java.util.List;
import java.util.Optional;

public interface ProductService {
	List<Product> findAll();

	Optional<Product> findById(Long id);

	Product save(ProductDto productDto); // Handles create/update including thresholds

	void deleteById(Long id);

	List<Product> findByCategory(Long categoryId);

	List<Product> searchProducts(String keyword);

	// --- NEW METHOD ---
	/**
	 * Adjusts the stock quantity of a specific product.
	 *
	 * @param productId      The ID of the product to adjust.
	 * @param quantityChange The amount to change the stock by (positive to add,
	 *                       negative to remove).
	 * @param reason         A brief reason for the adjustment (e.g., "Manual
	 *                       Correction", "Wastage", "Production").
	 * @return The updated Product entity.
	 * @throws RuntimeException if the product is not found or stock goes below
	 *                          zero.
	 */
	Product adjustStock(Long productId, int quantityChange, String reason);
	// --- END NEW ---
}