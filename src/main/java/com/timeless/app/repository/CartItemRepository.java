package com.timeless.app.repository;

import com.timeless.app.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByBuyerId(Long buyerId);

    Optional<CartItem> findByBuyerIdAndWatchId(Long buyerId, Long watchId);

    void deleteByBuyerIdAndWatchId(Long buyerId, Long watchId);

    void deleteByBuyerId(Long buyerId);

    void deleteByWatchId(Long watchId);

    long countByBuyerId(Long buyerId);
}
