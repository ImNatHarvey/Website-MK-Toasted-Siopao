package com.toastedsiopao.service;

import com.toastedsiopao.model.CartItem;
import com.toastedsiopao.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CartService {

    List<CartItem> getCartForUser(User user);

    CartItem addItemToCart(User user, Long productId, int quantity);

    CartItem updateItemQuantity(User user, Long productId, int newQuantity);

    void removeItemFromCart(User user, Long productId);

    void clearCart(User user);

    BigDecimal getCartTotal(List<CartItem> cartItems);

    int getCartItemCount(List<CartItem> cartItems);

    Map<String, Object> getCartSummary(User user);
}