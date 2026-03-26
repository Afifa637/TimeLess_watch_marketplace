package com.timeless.app.service;

import com.timeless.app.dto.request.CartCheckoutRequest;
import com.timeless.app.dto.request.OrderCreateRequest;
import com.timeless.app.dto.request.PaymentRequest;
import com.timeless.app.dto.response.CartItemResponse;
import com.timeless.app.dto.response.OrderResponse;
import com.timeless.app.entity.*;
import com.timeless.app.exception.*;
import com.timeless.app.repository.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final WatchRepository watchRepository;
    private final OrderService orderService;
    private final PaymentService paymentService;

    @Transactional
    public CartItemResponse addToCart(Long watchId, Long buyerId) {
        UserAccount buyer = getBuyer(buyerId);
        Watch watch = watchRepository.findById(watchId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch not found"));
        if (watch.getStatus() != WatchStatus.ACTIVE)
            throw new BadRequestException("Only active watches can be added to cart");
        int stock = watch.getStockQuantity() == null ? 0 : watch.getStockQuantity();
        if (stock < OrderService.STOCK_THRESHOLD)
            throw new BadRequestException("Watch is out of stock");

        CartItem item = cartItemRepository.findByBuyerIdAndWatchId(buyerId, watchId)
                .map(existing -> {
                    int next = existing.getQuantity() + 1;
                    if (next > stock)
                        throw new BadRequestException("Quantity exceeds available stock");
                    existing.setQuantity(next);
                    return existing;
                })
                .orElseGet(() -> CartItem.builder().buyer(buyer).watch(watch).quantity(1).build());

        return toResponse(cartItemRepository.save(item));
    }

    @Transactional
    public CartItemResponse updateCartQuantity(Long watchId, Long buyerId, int delta) {
        getBuyer(buyerId);
        CartItem item = cartItemRepository.findByBuyerIdAndWatchId(buyerId, watchId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"));
        int newQty = item.getQuantity() + delta;
        if (newQty <= 0) {
            cartItemRepository.delete(item);
            return null;
        }
        Watch watch = item.getWatch();
        int stock = watch.getStockQuantity() == null ? 0 : watch.getStockQuantity();
        if (newQty > stock)
            throw new BadRequestException("Quantity exceeds available stock (" + stock + ")");
        item.setQuantity(newQty);
        return toResponse(cartItemRepository.save(item));
    }

    public List<CartItemResponse> getCart(Long buyerId) {
        getBuyer(buyerId);
        return cartItemRepository.findByBuyerId(buyerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void removeFromCart(Long watchId, Long buyerId) {
        getBuyer(buyerId);
        cartItemRepository.deleteByBuyerIdAndWatchId(buyerId, watchId);
    }

    @Transactional
    public void clearCart(Long buyerId) {
        getBuyer(buyerId);
        cartItemRepository.deleteByBuyerId(buyerId);
    }

    /**
     * Checkout: create one Order per cart item (with quantity), then one Payment
     * for each order using the buyer-supplied method and account reference.
     */
    @Transactional
    public List<OrderResponse> checkout(Long buyerId, CartCheckoutRequest checkoutReq) {
        getBuyer(buyerId);
        List<CartItem> items = cartItemRepository.findByBuyerId(buyerId);
        if (items.isEmpty()) throw new BadRequestException("Cart is empty");

        List<OrderResponse> orders = new ArrayList<>();
        for (CartItem item : items) {
            OrderCreateRequest req = new OrderCreateRequest(item.getWatch().getId(), item.getQuantity());
            OrderResponse created = orderService.placeOrder(req, buyerId);

            // simulate payment immediately
            PaymentRequest payReq = new PaymentRequest(
                    created.getId(),
                    checkoutReq.getMethod(),
                    checkoutReq.getPaymentAccountRef()
            );
            paymentService.initiatePayment(payReq, buyerId);
            // re-fetch to include payment method in response
            orders.add(orderService.getOrderById(created.getId(), buyerId, Role.BUYER));
        }
        cartItemRepository.deleteByBuyerId(buyerId);
        return orders;
    }

    public long countCartItems(Long buyerId) {
        return cartItemRepository.findByBuyerId(buyerId).stream()
                .mapToLong(CartItem::getQuantity).sum();
    }

    private UserAccount getBuyer(Long buyerId) {
        UserAccount buyer = userAccountRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        if (buyer.getRole() != Role.BUYER)
            throw new ForbiddenException("Only buyers can use the cart");
        return buyer;
    }

    private CartItemResponse toResponse(CartItem item) {
        BigDecimal subtotal = item.getWatch().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartItemResponse.builder()
                .id(item.getId())
                .watchId(item.getWatch().getId())
                .watchName(item.getWatch().getName())
                .watchBrand(item.getWatch().getBrand())
                .watchImageUrl(item.getWatch().getImageUrl())
                .price(item.getWatch().getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
