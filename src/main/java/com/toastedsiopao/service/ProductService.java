package com.toastedsiopao.service;

import com.toastedsiopao.dto.ProductDto;
import com.toastedsiopao.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductService {
	List<Product> findAll();

	Optional<Product> findById(Long id);

	Product save(ProductDto productDto); // Save/Update using DTO

	void deleteById(Long id);

	List<Product> findByCategory(Long categoryId);

	List<Product> searchProducts(String keyword);
	// Add more specific search/filter methods if needed
}
