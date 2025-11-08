package com.toastedsiopao.service;

import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductService {
	Page<Product> findAll(Pageable pageable);

	Optional<Product> findById(Long id);

	Product save(ProductDto productDto);

	void deleteById(Long id);

	Page<Product> findByCategory(Long categoryId, Pageable pageable);

	Page<Product> searchProducts(String keyword, Pageable pageable);

	Page<Product> searchProducts(String keyword, Long categoryId, Pageable pageable);

	Product adjustStock(Long productId, int quantityChange, String reason);

	long countAllProducts();

	long countLowStockProducts();

	long countOutOfStockProducts();

	int calculateMaxProducible(Long productId);

}