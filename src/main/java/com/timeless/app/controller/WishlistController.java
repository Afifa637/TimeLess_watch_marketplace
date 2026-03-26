package com.timeless.app.controller;

import com.timeless.app.dto.response.CartItemResponse;
import com.timeless.app.dto.response.WishlistItemResponse;
import com.timeless.app.security.UserPrincipal;
import com.timeless.app.service.WishlistService;
import com.timeless.app.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Buyer wishlist management")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Get current buyer's wishlist")
    public ResponseEntity<List<WishlistItemResponse>> getWishlist() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(wishlistService.getWishlist(user.getId()));
    }

    @PostMapping("/{watchId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Add a watch to wishlist")
    public ResponseEntity<WishlistItemResponse> addToWishlist(@PathVariable Long watchId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(wishlistService.addToWishlist(watchId, user.getId()));
    }

    @DeleteMapping("/{watchId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Remove a watch from wishlist")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long watchId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        wishlistService.removeFromWishlist(watchId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{watchId}/move-to-cart")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Move a watch from wishlist to cart")
    public ResponseEntity<CartItemResponse> moveToCart(@PathVariable Long watchId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(wishlistService.moveToCart(watchId, user.getId()));
    }
}
