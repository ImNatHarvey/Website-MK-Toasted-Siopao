package com.toastedsiopao.service;

import com.toastedsiopao.model.CartItem;
import com.toastedsiopao.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CartService {

    /**
     * Retrieves all cart items for a specific user.
     * @param user The user whose cart is being retrieved.
     * @return A list of CartItem objects, eagerly fetching product info.
     */
    List<CartItem> getCartForUser(User user);

    /**
     * Adds a product to the user's cart or increments its quantity if it already exists.
     * @param user The user adding the item.
     * @param productId The ID of the product to add.
     * @param quantity The quantity to add.
     * @return The saved CartItem.
     * @throws IllegalArgumentException if stock is insufficient or product not found.
     */
    CartItem addItemToCart(User user, Long productId, int quantity);

    /**
     * Updates the quantity of an item already in the cart.
     * @param user The user whose cart is being updated.
     * @param productId The ID of the product to update.
     * @param newQuantity The new quantity.
     * @return The updated CartItem.
     * @throws IllegalArgumentException if stock is insufficient or item not in cart.
     */
    CartItem updateItemQuantity(User user, Long productId, int newQuantity);

    /**
     * Removes a product entirely from the user's cart.
     * @param user The user whose cart is being modified.
     * @param productId The ID of the product to remove.
     * @throws IllegalArgumentException if item not in cart.
     */
    void removeItemFromCart(User user, Long productId);

    /**
     * Removes all items from a user's cart.
     * @param user The user whose cart is being cleared.
     */
    void clearCart(User user);

    /**
     * Calculates the total price of all items in the user's cart.
     * @param cartItems A list of cart items (typically from getCartForUser).
     * @return The total price as a BigDecimal.
     */
    BigDecimal getCartTotal(List<CartItem> cartItems);

    /**
     * Gets the total number of items (sum of quantities) in the cart.
     * @param cartItems A list of cart items.
     * @return The total item count.
     */
    int getCartItemCount(List<CartItem> cartItems);

    /**
     * Builds a map containing cart summary (total price and item count).
     * @param user The user to build the summary for.
     * @return A Map with keys "totalPrice" (BigDecimal) and "itemCount" (Integer).
     */
    Map<String, Object> getCartSummary(User user);
}