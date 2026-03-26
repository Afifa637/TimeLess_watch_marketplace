package com.timeless.app.service;

import com.timeless.app.dto.request.PaymentRequest;
import com.timeless.app.dto.response.PaymentResponse;
import com.timeless.app.entity.*;
import com.timeless.app.exception.*;
import com.timeless.app.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WatchRepository watchRepository;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest req, Long buyerId) {
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!Objects.equals(order.getBuyer().getId(), buyerId))
            throw new ForbiddenException("You can only pay for your own order");
        if (order.getStatus() != OrderStatus.PENDING)
            throw new BadRequestException("Order is not in PENDING state");

        paymentRepository.findByOrderId(order.getId()).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.COMPLETED)
                throw new BadRequestException("Already paid");
            throw new BadRequestException("Payment already exists for this order");
        });

        Payment payment = Payment.builder()
                .order(order)
                .buyer(order.getBuyer())
                .amount(order.getTotalAmount())
                .method(parseMethod(req.getMethod()))
                .status(PaymentStatus.COMPLETED)
                .paymentAccountRef(req.getPaymentAccountRef())
                .paidAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        return toResponse(saved);
    }

    public PaymentResponse getPaymentByOrder(Long orderId, Long currentUserId, Role role) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (role == Role.ADMIN) return toResponse(payment);
        if (role == Role.BUYER && Objects.equals(payment.getBuyer().getId(), currentUserId))
            return toResponse(payment);
        throw new ForbiddenException("You do not have permission to view this payment");
    }

    @Transactional
    public PaymentResponse refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() == PaymentStatus.REFUNDED)
            throw new BadRequestException("Payment has already been refunded");

        payment.setStatus(PaymentStatus.REFUNDED);
        Order order = payment.getOrder();
        if (order.getStatus() != OrderStatus.CANCELLED) {
            order.setStatus(OrderStatus.CANCELLED);
            restoreWatchStock(order.getWatch().getId(),
                    order.getQuantity() == null ? 1 : order.getQuantity());
            orderRepository.save(order);
        }
        return toResponse(paymentRepository.save(payment));
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    private void restoreWatchStock(Long watchId, int qty) {
        Watch watch = watchRepository.findLockedById(watchId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch not found"));
        int current = watch.getStockQuantity() == null ? 0 : watch.getStockQuantity();
        watch.setStockQuantity(current + qty);
        if (watch.getStatus() == WatchStatus.INACTIVE) watch.setStatus(WatchStatus.ACTIVE);
        watchRepository.save(watch);
    }

    private PaymentMethod parseMethod(String method) {
        try { return PaymentMethod.valueOf(method.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new BadRequestException("Invalid payment method: " + method); }
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .buyerName(p.getBuyer().getFullName())
                .amount(p.getAmount())
                .method(p.getMethod().name())
                .status(p.getStatus().name())
                .transactionRef(p.getTransactionRef())
                .paymentAccountRef(p.getPaymentAccountRef())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
