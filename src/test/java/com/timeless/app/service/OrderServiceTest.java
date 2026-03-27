package com.timeless.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.timeless.app.dto.request.OrderCreateRequest;
import com.timeless.app.dto.request.OrderStatusUpdateRequest;
import com.timeless.app.dto.response.OrderResponse;
import com.timeless.app.entity.Order;
import com.timeless.app.entity.OrderStatus;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.entity.Watch;
import com.timeless.app.entity.WatchCondition;
import com.timeless.app.entity.WatchStatus;
import com.timeless.app.exception.BadRequestException;
import com.timeless.app.exception.ForbiddenException;
import com.timeless.app.repository.OrderRepository;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.repository.WatchRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private WatchRepository watchRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, userAccountRepository, watchRepository);
    }

    @Test
    void placeOrder_validRequest_reducesStockAndCreatesOrder() {
        // stock = 5, which is >= STOCK_THRESHOLD (3), so purchase is allowed
        UserAccount buyer = buyer(1L);
        Watch watch = activeWatch(10L, 5);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(watchRepository.findLockedById(10L)).thenReturn(Optional.of(watch));
        when(watchRepository.save(any(Watch.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(100L);
            return o;
        });

        OrderResponse response = orderService.placeOrder(new OrderCreateRequest(10L, 1), 1L);

        assertThat(watch.getStockQuantity()).isEqualTo(4); // 5 - 1 = 4
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void placeOrder_outOfStock_throwsBadRequestException() {
        // stock = 0 → below STOCK_THRESHOLD (3) → blocked
        UserAccount buyer = buyer(1L);
        Watch watch = activeWatch(10L, 0);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(watchRepository.findLockedById(10L)).thenReturn(Optional.of(watch));

        assertThatThrownBy(() -> orderService.placeOrder(new OrderCreateRequest(10L, 1), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("out of stock");
    }

    @Test
    void placeOrder_watchNotActive_throwsBadRequestException() {
        UserAccount buyer = buyer(1L);
        Watch watch = activeWatch(10L, 5);
        watch.setStatus(WatchStatus.REJECTED);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(watchRepository.findLockedById(10L)).thenReturn(Optional.of(watch));

        assertThatThrownBy(() -> orderService.placeOrder(new OrderCreateRequest(10L, 1), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void placeOrder_notABuyer_throwsForbiddenException() {
        UserAccount seller = UserAccount.builder().id(1L).role(Role.SELLER).enabled(true).build();
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> orderService.placeOrder(new OrderCreateRequest(10L, 1), 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getOrderById_buyerSeesOwnOrder_success() {
        Order order = sampleOrder(55L, 1L, 2L, OrderStatus.PENDING);
        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(55L, 1L, Role.BUYER);

        assertThat(response.getId()).isEqualTo(55L);
        assertThat(response.getBuyerId()).isEqualTo(1L);
    }

    @Test
    void getOrderById_buyerSeesOtherOrder_throwsForbiddenException() {
        Order order = sampleOrder(55L, 1L, 2L, OrderStatus.PENDING);
        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById(55L, 99L, Role.BUYER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateOrderStatus_buyerCancelsPendingOrder_restoresStock() {
        Order order = sampleOrder(55L, 1L, 2L, OrderStatus.PENDING);
        Watch watch = order.getWatch();
        watch.setStockQuantity(0);
        watch.setStatus(WatchStatus.INACTIVE);

        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));
        when(watchRepository.findLockedById(2L)).thenReturn(Optional.of(watch));
        when(watchRepository.save(any(Watch.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(
                55L,
                OrderStatusUpdateRequest.builder().status("CANCELLED").build(),
                1L,
                Role.BUYER
        );

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(watch.getStockQuantity()).isEqualTo(1); // 0 + order.quantity(1) = 1
        assertThat(watch.getStatus()).isEqualTo(WatchStatus.ACTIVE);
    }

    @Test
    void updateOrderStatus_buyerCancelsShippedOrder_throwsBadRequestException() {
        Order order = sampleOrder(55L, 1L, 2L, OrderStatus.SHIPPED);
        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(
                55L,
                OrderStatusUpdateRequest.builder().status("CANCELLED").build(),
                1L,
                Role.BUYER
        )).isInstanceOf(BadRequestException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserAccount buyer(Long id) {
        return UserAccount.builder()
                .id(id).role(Role.BUYER)
                .fullName("Buyer").email("buyer@test.com").enabled(true)
                .build();
    }

    private Watch activeWatch(Long id, int stock) {
        return Watch.builder()
                .id(id)
                .seller(UserAccount.builder().id(3L).fullName("Seller").role(Role.SELLER).build())
                .name("Watch").brand("Omega").category("Sport")
                .condition(WatchCondition.GOOD)
                .price(new BigDecimal("4500.00"))
                .stockQuantity(stock)
                .status(WatchStatus.ACTIVE)
                .reviews(new java.util.ArrayList<>())
                .build();
    }

    private Order sampleOrder(Long orderId, Long buyerId, Long watchId, OrderStatus status) {
        return Order.builder()
                .id(orderId)
                .buyer(buyer(buyerId))
                .watch(activeWatch(watchId, 5))
                .status(status)
                .quantity(1)
                .totalAmount(new BigDecimal("4500.00"))
                .build();
    }
}