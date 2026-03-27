package com.timeless.app.controller;

import com.timeless.app.dto.response.OrderResponse;
import com.timeless.app.dto.response.UserResponse;
import com.timeless.app.dto.response.WatchResponse;
import com.timeless.app.entity.PaymentStatus;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.entity.WatchStatus;
import com.timeless.app.repository.PaymentRepository;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.repository.WatchRepository;
import com.timeless.app.security.UserPrincipal;
import com.timeless.app.service.OrderService;
import com.timeless.app.service.ReviewService;
import com.timeless.app.service.WatchService;
import com.timeless.app.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative management endpoints")
public class AdminController {

    private final UserAccountRepository userAccountRepository;
    private final WatchRepository watchRepository;
    private final PaymentRepository paymentRepository;
    private final WatchService watchService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    @GetMapping("/users")
    @Operation(summary = "Get all users, optionally filtered by role")
    public ResponseEntity<List<UserResponse>> getAllUsers(@RequestParam(required = false) String role) {
        List<UserAccount> users;
        if (role == null || role.isBlank()) {
            users = userAccountRepository.findAll();
        } else {
            users = userAccountRepository.findByRole(Role.valueOf(role.trim().toUpperCase(Locale.ROOT)));
        }
        return ResponseEntity.ok(users.stream().map(this::toUserResponse).toList());
    }

    @PatchMapping("/users/{id}/toggle")
    @Operation(summary = "Toggle user enabled or disabled")
    public ResponseEntity<UserResponse> toggleUserEnabled(@PathVariable Long id) {
        UserPrincipal currentAdmin = SecurityUtils.getCurrentUser();
        UserAccount user = userAccountRepository.findById(id)
            .orElseThrow(() -> new com.timeless.app.exception.ResourceNotFoundException("User not found"));
        if (user.getId().equals(currentAdmin.getId())) {
            throw new com.timeless.app.exception.BadRequestException("You cannot disable your own admin account");
        }
        user.setEnabled(!user.isEnabled());
        return ResponseEntity.ok(toUserResponse(userAccountRepository.save(user)));
    }

    @GetMapping("/watches")
    @Operation(summary = "Get all watches as admin")
    public ResponseEntity<List<WatchResponse>> getAllWatches(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return ResponseEntity.ok(watchService.getAllWatchesForAdmin());
        }
        return ResponseEntity.ok(watchService.getWatchesByStatus(WatchStatus.valueOf(status.trim().toUpperCase(Locale.ROOT))));
    }

    @GetMapping("/watches/pending")
    @Operation(summary = "Get watches pending review")
    public ResponseEntity<List<WatchResponse>> getPendingWatches() {
        return ResponseEntity.ok(watchService.getPendingWatches());
    }

    @PatchMapping("/watches/{id}/approve")
    @Operation(summary = "Approve a watch listing")
    public ResponseEntity<WatchResponse> approveWatch(@PathVariable Long id) {
        return ResponseEntity.ok(watchService.approveWatch(id));
    }

    @PatchMapping("/watches/{id}/reject")
    @Operation(summary = "Reject a watch listing")
    public ResponseEntity<WatchResponse> rejectWatch(@PathVariable Long id) {
        return ResponseEntity.ok(watchService.rejectWatch(id));
    }

    @PatchMapping("/watches/{id}/deactivate")
    @Operation(summary = "Deactivate a watch listing")
    public ResponseEntity<WatchResponse> deactivateWatch(@PathVariable Long id) {
        return ResponseEntity.ok(watchService.deactivateWatch(id));
    }

    @DeleteMapping("/watches/{id}")
    @Operation(summary = "Delete any watch as admin")
    public ResponseEntity<Void> deleteWatch(@PathVariable Long id) {
        UserPrincipal admin = SecurityUtils.getCurrentUser();
        watchService.deleteWatch(id, admin.getId(), admin.getRole());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    @Operation(summary = "Get all orders as admin")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrdersAdmin());
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Delete a review as admin")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        UserPrincipal admin = SecurityUtils.getCurrentUser();
        reviewService.deleteReview(id, admin.getId(), admin.getRole());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get marketplace admin stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        BigDecimal totalRevenue = paymentRepository.findAll().stream()
            .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
            .map(payment -> payment.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userAccountRepository.count());
        stats.put("totalWatches", watchRepository.count());
        stats.put("totalOrders", orderService.getAllOrdersAdmin().size());
        stats.put("pendingReviews", watchRepository.countByStatus(WatchStatus.PENDING_REVIEW));
        stats.put("totalRevenue", totalRevenue);
        return ResponseEntity.ok(stats);
    }

    private UserResponse toUserResponse(UserAccount user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .address(user.getAddress())
            .role(user.getRole().name())
            .enabled(user.isEnabled())
            .emailVerified(user.isEmailVerified())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
