package com.timeless.app.repository;

import com.timeless.app.entity.Order;
import com.timeless.app.entity.OrderStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerId(Long buyerId);

    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<Order> findByWatchSellerId(Long sellerId);

    List<Order> findByWatchSellerIdOrderByCreatedAtDesc(Long sellerId);

    List<Order> findAllByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);

    List<Order> findByWatchId(Long watchId);

    boolean existsByWatchIdAndStatusIn(Long watchId, Collection<OrderStatus> statuses);
}
