package com.toastedsiopao.service;

import com.toastedsiopao.model.CartItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.CartItemRepository;
import com.toastedsiopao.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartForUser(User user) {
        return cartItemRepository.findByUserWithProduct(user);
    }

    @Override
    public CartItem addItemToCart(User user, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (!"ACTIVE".equals(product.getProductStatus()) || product.getCurrentStock() <= 0) {
            throw new IllegalArgumentException("Product '" + product.getName() + "' is not available.");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserAndProduct(user, product);

        CartItem cartItem;
        if (existingItemOpt.isPresent()) {
            cartItem = existingItemOpt.get();
            int newQuantity = cartItem.getQuantity() + quantity;
            
            if (product.getCurrentStock() < newQuantity) {
                throw new IllegalArgumentException("Insufficient stock for: " + product.getName() + 
                                                   ". Requested: " + newQuantity + ", Available: " + product.getCurrentStock());
            }
            cartItem.setQuantity(newQuantity);
            log.info("Incremented quantity for product {} in user {}'s cart to {}", productId, user.getUsername(), newQuantity);
        } else {
            if (product.getCurrentStock() < quantity) {
                throw new IllegalArgumentException("Insufficient stock for: " + product.getName() + 
                                                   ". Requested: " + quantity + ", Available: " + product.getCurrentStock());
            }
            cartItem = new CartItem(user, product, quantity);
            log.info("Added new product {} to user {}'s cart with quantity {}", productId, user.getUsername(), quantity);
        }

        return cartItemRepository.save(cartItem);
    }

    @Override
    public CartItem updateItemQuantity(User user, Long productId, int newQuantity) {
        if (newQuantity <= 0) {
            removeItemFromCart(user, productId);
            return null;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new IllegalArgumentException("Item not found in cart."));

        if (product.getCurrentStock() < newQuantity) {
            throw new IllegalArgumentException("Insufficient stock for: " + product.getName() + 
                                               ". Requested: " + newQuantity + ", Available: " + product.getCurrentStock());
        }

        cartItem.setQuantity(newQuantity);
        log.info("Updated quantity for product {} in user {}'s cart to {}", productId, user.getUsername(), newQuantity);
        return cartItemRepository.save(cartItem);
    }

    @Override
    public void removeItemFromCart(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new IllegalArgumentException("Item not found in cart."));

        cartItemRepository.delete(cartItem);
        log.info("Removed product {} from user {}'s cart", productId, user.getUsername());
    }

    @Override
    public void clearCart(User user) {
        cartItemRepository.deleteByUser(user);
        log.info("Cleared all cart items for user {}", user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return 0;
        }
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCartSummary(User user) {
        List<CartItem> cartItems = getCartForUser(user);
        BigDecimal totalPrice = getCartTotal(cartItems);
        int itemCount = getCartItemCount(cartItems);
        return Map.of("totalPrice", totalPrice, "itemCount", itemCount);
    }
}