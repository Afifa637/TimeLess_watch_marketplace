package com.timeless.app.service;

import com.timeless.app.dto.request.OrderCreateRequest;
import com.timeless.app.dto.request.OrderStatusUpdateRequest;
import com.timeless.app.dto.response.OrderResponse;
import com.timeless.app.entity.*;
import com.timeless.app.exception.*;
import com.timeless.app.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    /** Minimum stock required for a buyer to purchase. Below this: "Stock Out". */
    public static final int STOCK_THRESHOLD = 3;

    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final WatchRepository watchRepository;

    @Transactional
    public OrderResponse placeOrder(OrderCreateRequest req, Long buyerId) {
        int qty = (req.getQuantity() == null || req.getQuantity() < 1) ? 1 : req.getQuantity();

        UserAccount buyer = userAccountRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        if (buyer.getRole() != Role.BUYER)
            throw new ForbiddenException("Only buyers can place orders");

        Watch watch = watchRepository.findLockedById(req.getWatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Watch not found"));
        if (watch.getStatus() != WatchStatus.ACTIVE)
            throw new BadRequestException("Watch is not available for purchase");

        int stock = watch.getStockQuantity() == null ? 0 : watch.getStockQuantity();
        if (stock < STOCK_THRESHOLD)
            throw new BadRequestException("This product is currently out of stock");
        if (qty > stock)
            throw new BadRequestException("Requested quantity (" + qty + ") exceeds available stock (" + stock + ")");

        watch.setStockQuantity(stock - qty);
        watchRepository.save(watch);

        Order order = Order.builder()
                .buyer(buyer)
                .watch(watch)
                .status(OrderStatus.PENDING)
                .quantity(qty)
                .totalAmount(watch.getPrice().multiply(java.math.BigDecimal.valueOf(qty)))
                .build();
        return toResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getMyOrdersAsBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getOrdersForSeller(Long sellerId) {
        return orderRepository.findByWatchSellerIdOrderByCreatedAtDesc(sellerId)
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getAllOrdersAdmin() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public OrderResponse getOrderById(Long orderId, Long currentUserId, Role role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateOrderAccess(order, currentUserId, role);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest req,
                                           Long currentUserId, Role role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus targetStatus = parseStatus(req.getStatus());
        switch (role) {
            case BUYER  -> handleBuyerTransition(order, targetStatus, currentUserId);
            case SELLER -> handleSellerTransition(order, targetStatus, req.getTrackingNumber(), currentUserId);
            case ADMIN  -> throw new ForbiddenException("Admins view orders but cannot change status");
            default     -> throw new ForbiddenException("Invalid role");
        }
        return toResponse(orderRepository.save(order));
    }

    public Optional<Order> findReviewableOrderForBuyerAndWatch(Long buyerId, Long watchId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .filter(o -> Objects.equals(o.getWatch().getId(), watchId))
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .findFirst();
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private void handleBuyerTransition(Order order, OrderStatus target, Long userId) {
        if (!Objects.equals(order.getBuyer().getId(), userId))
            throw new ForbiddenException("You can only update your own orders");
        if (target == OrderStatus.CANCELLED) {
            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED)
                throw new BadRequestException("Order can only be cancelled before processing");
            cancelAndRestore(order);
            return;
        }
        if (target == OrderStatus.COMPLETED) {
            if (order.getStatus() != OrderStatus.SHIPPED)
                throw new BadRequestException("You can confirm receipt only after the order has been shipped");
            order.setStatus(OrderStatus.COMPLETED);
            return;
        }
        throw new BadRequestException("Buyer can only cancel a pending order or confirm receipt of a shipped order");
    }

    private void handleSellerTransition(Order order, OrderStatus target, String tracking, Long userId) {
        if (!Objects.equals(order.getWatch().getSeller().getId(), userId))
            throw new ForbiddenException("You can only update orders for your own watches");
        if (order.getStatus() == OrderStatus.CONFIRMED && target == OrderStatus.PROCESSING) {
            order.setStatus(OrderStatus.PROCESSING); return;
        }
        if (order.getStatus() == OrderStatus.PROCESSING && target == OrderStatus.SHIPPED) {
            if (tracking == null || tracking.isBlank())
                throw new BadRequestException("Tracking number is required when shipping an order");
            order.setTrackingNumber(tracking.trim());
            order.setStatus(OrderStatus.SHIPPED); return;
        }
        throw new BadRequestException("Invalid status transition for seller");
    }

    private void cancelAndRestore(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        order.setStatus(OrderStatus.CANCELLED);
        restoreWatchStock(order.getWatch().getId(), order.getQuantity() == null ? 1 : order.getQuantity());
    }

    private void restoreWatchStock(Long watchId, int qty) {
        Watch watch = watchRepository.findLockedById(watchId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch not found"));
        int current = watch.getStockQuantity() == null ? 0 : watch.getStockQuantity();
        watch.setStockQuantity(current + qty);
        // Reactivate if it was inactive due to zero stock
        if (watch.getStatus() == WatchStatus.INACTIVE) watch.setStatus(WatchStatus.ACTIVE);
        watchRepository.save(watch);
    }

    private void validateOrderAccess(Order order, Long userId, Role role) {
        if (role == Role.ADMIN) return;
        if (role == Role.BUYER && Objects.equals(order.getBuyer().getId(), userId)) return;
        if (role == Role.SELLER && Objects.equals(order.getWatch().getSeller().getId(), userId)) return;
        throw new ForbiddenException("You do not have permission to view this order");
    }

    private OrderStatus parseStatus(String s) {
        try { return OrderStatus.valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new BadRequestException("Invalid order status"); }
    }

    OrderResponse toResponse(Order order) {
        String paymentMethod = null;
        if (order.getPayment() != null) paymentMethod = order.getPayment().getMethod().name();
        return OrderResponse.builder()
                .id(order.getId())
                .buyerId(order.getBuyer().getId())
                .buyerName(order.getBuyer().getFullName())
                .watchId(order.getWatch().getId())
                .watchName(order.getWatch().getName())
                .watchBrand(order.getWatch().getBrand())
                .watchImageUrl(order.getWatch().getImageUrl())
                .sellerName(order.getWatch().getSeller().getFullName())
                .status(order.getStatus().name())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .trackingNumber(order.getTrackingNumber())
                .paymentMethod(paymentMethod)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
