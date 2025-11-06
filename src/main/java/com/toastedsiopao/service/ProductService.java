package com.toastedsiopao.service;

import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.model.Product;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable

import java.math.BigDecimal; // Import BigDecimal
import java.util.List; // REMOVED (no longer returning List)
import java.util.Optional;

public interface ProductService {
	Page<Product> findAll(Pageable pageable); // Updated

	Optional<Product> findById(Long id);

	Product save(ProductDto productDto); // Handles create/update including thresholds

	void deleteById(Long id);

	Page<Product> findByCategory(Long categoryId, Pageable pageable); // Updated

	Page<Product> searchProducts(String keyword, Pageable pageable); // Updated

	// --- NEW: Combined search method ---
	/**
	 * Searches for products based on a keyword and/or category, with pagination.
	 * * @param keyword The search term (matches product name). * @param categoryId
	 * The category ID to filter by.
	 * 
	 * @param pageable The pagination information.
	 * @return A paginated list of matching products.
	 */
	Page<Product> searchProducts(String keyword, Long categoryId, Pageable pageable);

	// --- FIX: Re-added the missing method signature ---
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
	// --- END FIX ---

	// --- NEW: Methods for stat cards ---
	long countAllProducts();

	long countLowStockProducts();

	long countOutOfStockProducts();
	// --- END NEW ---
}