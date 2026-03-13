package com.timeless.app.repository;

import com.timeless.app.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByWatchId(Long watchId);

    List<Review> findByBuyerId(Long buyerId);

    boolean existsByBuyerIdAndWatchId(Long buyerId, Long watchId);

    boolean existsByBuyerIdAndOrderId(Long buyerId, Long orderId);

    Optional<Review> findByBuyerIdAndWatchId(Long buyerId, Long watchId);

    long countByWatchId(Long watchId);

    void deleteByWatchId(Long watchId);
}
