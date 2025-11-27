package com.toastedsiopao.service;

import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductService {
	Page<Product> findAll(Pageable pageable);

	Optional<Product> findById(Long id);

	Product save(ProductDto productDto);

	void deactivateProduct(Long id);

	void activateProduct(Long id);
	
	void deleteProduct(Long id); 

	Page<Product> findByCategory(Long categoryId, Pageable pageable);

	Page<Product> searchProducts(String keyword, Pageable pageable);

	Page<Product> searchProducts(String keyword, Long categoryId, Pageable pageable);
	
	Page<Product> searchAdminProducts(String keyword, Long categoryId, Pageable pageable);

	// --- MODIFIED: Added Date parameters ---
	Product adjustStock(Long productId, int quantityChange, String reason, LocalDate createdDate, Integer expirationDays);
	
	// Overload for backward compatibility or simple calls
	default Product adjustStock(Long productId, int quantityChange, String reason) {
		return adjustStock(productId, quantityChange, reason, null, null);
	}
	// --- END MODIFIED ---

	long countAllProducts();

	long countLowStockProducts();
	
	long countCriticalStockProducts();

	long countOutOfStockProducts();

	int calculateMaxProducible(Long productId);

	List<Product> findAllForReport(String keyword, Long categoryId);
}