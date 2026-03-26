package com.timeless.app.controller;

import com.timeless.app.dto.request.CartCheckoutRequest;
import com.timeless.app.dto.response.CartItemResponse;
import com.timeless.app.dto.response.OrderResponse;
import com.timeless.app.security.UserPrincipal;
import com.timeless.app.service.CartService;
import com.timeless.app.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Buyer cart management")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Get current buyer's cart")
    public ResponseEntity<List<CartItemResponse>> getCart() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(cartService.getCart(user.getId()));
    }

    @PostMapping("/{watchId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Add a watch to cart (+1)")
    public ResponseEntity<CartItemResponse> addToCart(@PathVariable Long watchId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(cartService.addToCart(watchId, user.getId()));
    }

    @PatchMapping("/{watchId}/quantity")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Update cart item quantity. Send {\"delta\": 1} or {\"delta\": -1}")
    public ResponseEntity<CartItemResponse> updateQuantity(
            @PathVariable Long watchId,
            @RequestBody Map<String, Integer> body) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        int delta = body.getOrDefault("delta", 0);
        CartItemResponse updated = cartService.updateCartQuantity(watchId, user.getId(), delta);
        if (updated == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{watchId}")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Remove a watch from cart entirely")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long watchId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        cartService.removeFromCart(watchId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Clear the entire cart")
    public ResponseEntity<Void> clearCart() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Checkout all cart items — creates orders and processes payment in one step")
    public ResponseEntity<List<OrderResponse>> checkout(
            @Valid @RequestBody CartCheckoutRequest checkoutRequest) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.checkout(user.getId(), checkoutRequest));
    }
}
