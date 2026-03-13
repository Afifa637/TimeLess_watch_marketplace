package com.timeless.app.repository;

import com.timeless.app.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByBuyerId(Long buyerId);

    boolean existsByOrderId(Long orderId);

    List<Payment> findAllByOrderByCreatedAtDesc();
}
