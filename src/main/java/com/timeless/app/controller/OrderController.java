package com.timeless.app.controller;

import com.timeless.app.dto.request.OrderCreateRequest;
import com.timeless.app.dto.request.OrderStatusUpdateRequest;
import com.timeless.app.dto.response.OrderResponse;
import com.timeless.app.security.UserPrincipal;
import com.timeless.app.service.OrderService;
import com.timeless.app.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and lifecycle management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Place an order for a watch")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderCreateRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request, user.getId()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Get current buyer's orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(orderService.getMyOrdersAsBuyer(user.getId()));
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get orders for the current seller's watches")
    public ResponseEntity<List<OrderResponse>> getSellerOrders() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(orderService.getOrdersForSeller(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get order detail by id")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(orderService.getOrderById(id, user.getId(), user.getRole()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update order status based on the current user's role")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request, user.getId(), user.getRole()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders as admin")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrdersAdmin());
    }
}
