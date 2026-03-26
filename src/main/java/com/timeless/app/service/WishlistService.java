package com.timeless.app.service;

import com.timeless.app.dto.response.CartItemResponse;
import com.timeless.app.dto.response.WishlistItemResponse;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.entity.Watch;
import com.timeless.app.entity.WatchStatus;
import com.timeless.app.entity.WishlistItem;
import com.timeless.app.exception.BadRequestException;
import com.timeless.app.exception.ForbiddenException;
import com.timeless.app.exception.ResourceNotFoundException;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.repository.WatchRepository;
import com.timeless.app.repository.WishlistItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final WatchRepository watchRepository;
    private final CartService cartService;

    @Transactional
    public WishlistItemResponse addToWishlist(Long watchId, Long buyerId) {
        UserAccount buyer = getBuyer(buyerId);
        Watch watch = watchRepository.findById(watchId)
            .orElseThrow(() -> new ResourceNotFoundException("Watch not found"));
        if (watch.getStatus() != WatchStatus.ACTIVE) {
            throw new BadRequestException("Only active watches can be added to wishlist");
        }
        if (wishlistItemRepository.existsByBuyerIdAndWatchId(buyerId, watchId)) {
            throw new BadRequestException("Already in wishlist");
        }

        WishlistItem item = WishlistItem.builder()
            .buyer(buyer)
            .watch(watch)
            .id(new WishlistItem.WishlistItemId(buyerId, watchId))
            .build();
        return toResponse(wishlistItemRepository.save(item));
    }

    public List<WishlistItemResponse> getWishlist(Long buyerId) {
        getBuyer(buyerId);
        return wishlistItemRepository.findByBuyerId(buyerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void removeFromWishlist(Long watchId, Long buyerId) {
        getBuyer(buyerId);
        wishlistItemRepository.deleteByBuyerIdAndWatchId(buyerId, watchId);
    }

    @Transactional
    public CartItemResponse moveToCart(Long watchId, Long buyerId) {
        CartItemResponse response = cartService.addToCart(watchId, buyerId);
        wishlistItemRepository.deleteByBuyerIdAndWatchId(buyerId, watchId);
        return response;
    }

    public long countWishlistItems(Long buyerId) {
        return wishlistItemRepository.countByBuyerId(buyerId);
    }

    private UserAccount getBuyer(Long buyerId) {
        UserAccount buyer = userAccountRepository.findById(buyerId)
            .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        if (buyer.getRole() != Role.BUYER) {
            throw new ForbiddenException("Only buyers can use the wishlist");
        }
        return buyer;
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        return WishlistItemResponse.builder()
            .id(item.getId().getUserId() + "-" + item.getId().getWatchId())
            .watchId(item.getWatch().getId())
            .watchName(item.getWatch().getName())
            .watchBrand(item.getWatch().getBrand())
            .watchImageUrl(item.getWatch().getImageUrl())
            .price(item.getWatch().getPrice())
            .watchStatus(item.getWatch().getStatus().name())
            .addedAt(item.getAddedAt())
            .build();
    }
}
