package com.timeless.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.timeless.app.dto.request.WatchCreateRequest;
import com.timeless.app.dto.request.WatchUpdateRequest;
import com.timeless.app.dto.response.WatchResponse;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.entity.Watch;
import com.timeless.app.entity.WatchCondition;
import com.timeless.app.entity.WatchStatus;
import com.timeless.app.exception.ForbiddenException;
import com.timeless.app.exception.ResourceNotFoundException;
import com.timeless.app.repository.CartItemRepository;
import com.timeless.app.repository.OrderRepository;
import com.timeless.app.repository.PaymentRepository;
import com.timeless.app.repository.ReviewRepository;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.repository.WatchRepository;
import com.timeless.app.repository.WishlistItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchServiceTest {

    @Mock private WatchRepository watchRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private WishlistItemRepository wishlistItemRepository;
    @Mock private ReviewRepository reviewRepository;

    private WatchService watchService;

    @BeforeEach
    void setUp() {
        watchService = new WatchService(
            watchRepository,
            userAccountRepository,
            orderRepository,
            paymentRepository,
            cartItemRepository,
            wishlistItemRepository,
            reviewRepository
        );
    }

    @Test
    void createWatch_asSeller_setsStatusToPendingReview() {
        UserAccount seller = seller(1L);
        WatchCreateRequest request = WatchCreateRequest.builder()
            .name("Rolex Datejust")
            .brand("Rolex")
            .category("Dress")
            .condition("LIKE_NEW")
            .price(new BigDecimal("5000.00"))
            .stockQuantity(1)
            .build();

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(watchRepository.save(any(Watch.class))).thenAnswer(invocation -> {
            Watch watch = invocation.getArgument(0);
            watch.setId(9L);
            return watch;
        });

        WatchResponse response = watchService.createWatch(request, 1L);

        assertThat(response.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(response.getSellerId()).isEqualTo(1L);
    }

    @Test
    void updateWatch_sellerUpdatesOwnWatch_success() {
        UserAccount seller = seller(1L);
        Watch watch = sampleWatch(10L, seller);
        WatchUpdateRequest request = WatchUpdateRequest.builder()
            .name("Updated Name")
            .price(new BigDecimal("6500.00"))
            .build();

        when(watchRepository.findById(10L)).thenReturn(Optional.of(watch));
        when(watchRepository.save(any(Watch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WatchResponse response = watchService.updateWatch(10L, request, 1L, Role.SELLER);

        assertThat(response.getName()).isEqualTo("Updated Name");
        assertThat(response.getPrice()).isEqualByComparingTo("6500.00");
        assertThat(response.getStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void updateWatch_sellerUpdatesOtherWatch_throwsForbiddenException() {
        Watch watch = sampleWatch(10L, seller(2L));

        when(watchRepository.findById(10L)).thenReturn(Optional.of(watch));

        assertThatThrownBy(() -> watchService.updateWatch(10L, new WatchUpdateRequest(), 1L, Role.SELLER))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteWatch_adminDeletesAnyWatch_success() {
        Watch watch = sampleWatch(10L, seller(2L));

        when(watchRepository.findById(10L)).thenReturn(Optional.of(watch));
        when(orderRepository.findByWatchId(10L)).thenReturn(List.of());

        watchService.deleteWatch(10L, 99L, Role.ADMIN);

        verify(watchRepository).delete(watch);
    }

    @Test
    void deleteWatch_sellerDeletesOwnWatch_noActiveOrders_success() {
        UserAccount seller = seller(1L);
        Watch watch = sampleWatch(10L, seller);

        when(watchRepository.findById(10L)).thenReturn(Optional.of(watch));
        when(orderRepository.existsByWatchIdAndStatusIn(any(), any())).thenReturn(false);
        when(orderRepository.findByWatchId(10L)).thenReturn(List.of());

        watchService.deleteWatch(10L, 1L, Role.SELLER);

        verify(watchRepository).delete(watch);
    }

    @Test
    void deleteWatch_sellerDeletesOtherWatch_throwsForbiddenException() {
        Watch watch = sampleWatch(10L, seller(2L));
        when(watchRepository.findById(10L)).thenReturn(Optional.of(watch));

        assertThatThrownBy(() -> watchService.deleteWatch(10L, 1L, Role.SELLER))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getWatchById_notFound_throwsResourceNotFoundException() {
        when(watchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchService.getWatchById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveWatch_setsStatusToActive() {
        Watch watch = sampleWatch(10L, seller(1L));
        watch.setStatus(WatchStatus.PENDING_REVIEW);

        when(watchRepository.findById(10L)).thenReturn(Optional.of(watch));
        when(watchRepository.save(any(Watch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WatchResponse response = watchService.approveWatch(10L);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    private UserAccount seller(Long id) {
        return UserAccount.builder().id(id).fullName("Seller").email("seller@test.com").role(Role.SELLER).enabled(true).build();
    }

    private Watch sampleWatch(Long id, UserAccount seller) {
        return Watch.builder()
            .id(id)
            .seller(seller)
            .name("Sample Watch")
            .brand("Rolex")
            .category("Luxury")
            .condition(WatchCondition.GOOD)
            .price(new BigDecimal("6000.00"))
            .stockQuantity(1)
            .status(WatchStatus.ACTIVE)
            .reviews(new java.util.ArrayList<>())
            .build();
    }
}
