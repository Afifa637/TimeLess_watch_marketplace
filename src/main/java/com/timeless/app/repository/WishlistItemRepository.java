package com.timeless.app.repository;

import com.timeless.app.entity.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, WishlistItem.WishlistItemId> {

    List<WishlistItem> findByBuyerId(Long buyerId);

    Optional<WishlistItem> findByBuyerIdAndWatchId(Long buyerId, Long watchId);

    boolean existsByBuyerIdAndWatchId(Long buyerId, Long watchId);

    void deleteByBuyerIdAndWatchId(Long buyerId, Long watchId);

    void deleteByWatchId(Long watchId);

    long countByBuyerId(Long buyerId);
}
