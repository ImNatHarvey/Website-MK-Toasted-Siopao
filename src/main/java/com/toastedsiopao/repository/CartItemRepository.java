package com.toastedsiopao.repository;

import com.toastedsiopao.model.CartItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Eagerly fetch product and its category for cart display
    @Query("SELECT ci FROM CartItem ci " +
           "JOIN FETCH ci.product p " +
           "LEFT JOIN FETCH p.category " + // --- THIS IS THE FIX ---
           "WHERE ci.user = :user " +
           "ORDER BY ci.lastUpdated DESC")
    List<CartItem> findByUserWithProduct(@Param("user") User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    @Modifying
    void deleteByUser(User user);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.user = :user AND ci.product = :product")
    void deleteByUserAndProduct(@Param("user") User user, @Param("product") Product product);
}