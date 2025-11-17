package com.toastedsiopao.controller;

import com.toastedsiopao.model.CartItem;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.CartService;
import com.toastedsiopao.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private CustomerService customerService;

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated.");
        }
        User user = customerService.findByUsername(principal.getName());
        if (user == null) {
            throw new IllegalStateException("Authenticated user not found in database.");
        }
        return user;
    }

    private Map<String, Object> buildCartResponse(User user) {
        List<CartItem> cartItems = cartService.getCartForUser(user);
        BigDecimal total = cartService.getCartTotal(cartItems);
        int totalItems = cartService.getCartItemCount(cartItems);

        List<Map<String, Object>> itemsList = cartItems.stream().map(item -> Map.<String, Object>of(
                "productId", item.getProduct().getId(),
                "name", item.getProduct().getName(),
                "price", item.getProduct().getPrice(),
                "quantity", item.getQuantity(),
                "image", item.getProduct().getImageUrl() != null ? item.getProduct().getImageUrl() : "/img/placeholder.jpg",
                "subtotal", item.getSubtotal(),
                "stock", item.getProduct().getCurrentStock()
        )).collect(Collectors.toList());

        return Map.of(
                "items", itemsList,
                "totalPrice", total,
                "totalItems", totalItems
        );
    }

    @GetMapping("/items")
    public ResponseEntity<?> getCartItems(Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            return ResponseEntity.ok(buildCartResponse(user));
        } catch (Exception e) {
            log.error("Error getting cart items for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Could not retrieve cart."));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItemToCart(@RequestBody Map<String, String> payload, Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            Long productId = Long.parseLong(payload.get("productId"));
            int quantity = Integer.parseInt(payload.get("quantity"));

            cartService.addItemToCart(user, productId, quantity);
            return ResponseEntity.ok(buildCartResponse(user));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to add item to cart for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding item to cart for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateItemQuantity(@RequestBody Map<String, String> payload, Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            Long productId = Long.parseLong(payload.get("productId"));
            int newQuantity = Integer.parseInt(payload.get("newQuantity"));

            cartService.updateItemQuantity(user, productId, newQuantity);
            return ResponseEntity.ok(buildCartResponse(user));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to update cart item for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating cart item for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeItemFromCart(@RequestBody Map<String, String> payload, Principal principal) {
         try {
            User user = getAuthenticatedUser(principal);
            Long productId = Long.parseLong(payload.get("productId"));

            cartService.removeItemFromCart(user, productId);
            return ResponseEntity.ok(buildCartResponse(user));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to remove cart item for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error removing cart item for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearCart(Principal principal) {
         try {
            User user = getAuthenticatedUser(principal);
            cartService.clearCart(user);
            return ResponseEntity.ok(buildCartResponse(user));

        } catch (Exception e) {
            log.error("Error clearing cart for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }
}