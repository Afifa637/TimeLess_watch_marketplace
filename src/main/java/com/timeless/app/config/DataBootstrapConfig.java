package com.timeless.app.config;

import com.timeless.app.entity.Order;
import com.timeless.app.entity.OrderStatus;
import com.timeless.app.entity.Payment;
import com.timeless.app.entity.PaymentMethod;
import com.timeless.app.entity.PaymentStatus;
import com.timeless.app.entity.Review;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.entity.Watch;
import com.timeless.app.entity.WatchCondition;
import com.timeless.app.entity.WatchStatus;
import com.timeless.app.repository.OrderRepository;
import com.timeless.app.repository.PaymentRepository;
import com.timeless.app.repository.ReviewRepository;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.repository.WatchRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataBootstrapConfig {

    private final UserAccountRepository userAccountRepository;
    private final WatchRepository watchRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.email:admin@timeless.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin123!}")
    private String adminPassword;

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            if (!bootstrapEnabled || userAccountRepository.count() > 0) {
                return;
            }

            UserAccount admin = createUser(adminEmail, adminPassword, "System Admin", Role.ADMIN, "0123456789", "Timeless HQ");
            UserAccount seller1 = createUser("seller1@timeless.com", "Seller123!", "Watch World Store", Role.SELLER, "01700000001", "Geneva Boutique Lane 1");
            UserAccount seller2 = createUser("seller2@timeless.com", "Seller123!", "Premier Timepieces", Role.SELLER, "01700000002", "Zurich Horology Avenue 8");
            UserAccount buyer1 = createUser("buyer1@timeless.com", "Buyer123!", "Alex Johnson", Role.BUYER, "01700000003", "12 Hudson Street, New York");
            UserAccount buyer2 = createUser("buyer2@timeless.com", "Buyer123!", "Sarah Williams", Role.BUYER, "01700000004", "44 Mayfair Square, London");

            userAccountRepository.saveAll(List.of(admin, seller1, seller2, buyer1, buyer2));

            List<Watch> watches = new ArrayList<>();
            watches.add(createWatch(seller1, "Rolex Submariner Date", "Rolex", "Dive", WatchCondition.LIKE_NEW, new BigDecimal("13500.00"), 3, WatchStatus.ACTIVE, "Iconic stainless steel dive watch with ceramic bezel.", "https://images.unsplash.com/photo-1547996160-81dfa63595aa?auto=format&fit=crop&w=900&q=80", "126610LN", 2022));
            watches.add(createWatch(seller1, "Omega Speedmaster Moonwatch", "Omega", "Chronograph", WatchCondition.GOOD, new BigDecimal("7200.00"), 2, WatchStatus.ACTIVE, "Manual-wind chronograph inspired by the legendary moonwatch heritage.", "https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?auto=format&fit=crop&w=900&q=80", "310.30.42.50.01.001", 2021));
            watches.add(createWatch(seller1, "Cartier Tank Must", "Cartier", "Dress", WatchCondition.NEW, new BigDecimal("4100.00"), 4, WatchStatus.ACTIVE, "Elegant rectangular dress watch in classic Cartier style.", "https://images.unsplash.com/photo-1434056886845-dac89ffe9b56?auto=format&fit=crop&w=900&q=80", "WSTA0041", 2024));
            watches.add(createWatch(seller1, "TAG Heuer Carrera Chronograph", "TAG Heuer", "Sport", WatchCondition.LIKE_NEW, new BigDecimal("5600.00"), 2, WatchStatus.ACTIVE, "Modern racing-inspired chronograph with bold blue dial.", "https://images.unsplash.com/photo-1524805444758-089113d48a6d?auto=format&fit=crop&w=900&q=80", "CBN2A1A.BA0643", 2023));
            watches.add(createWatch(seller2, "Patek Philippe Calatrava", "Patek Philippe", "Luxury", WatchCondition.GOOD, new BigDecimal("24800.00"), 2, WatchStatus.ACTIVE, "Refined minimalist dress watch with exhibition caseback.", "https://images.unsplash.com/photo-1508057198894-247b23fe5ade?auto=format&fit=crop&w=900&q=80", "6119G-001", 2020));
            watches.add(createWatch(seller2, "Audemars Piguet Royal Oak", "Audemars Piguet", "Luxury", WatchCondition.LIKE_NEW, new BigDecimal("38900.00"), 2, WatchStatus.ACTIVE, "Integrated bracelet icon finished in steel with tapisserie dial.", "https://images.unsplash.com/photo-1523170335258-f5ed11844a49?auto=format&fit=crop&w=900&q=80", "15500ST.OO.1220ST.04", 2022));
            watches.add(createWatch(seller2, "Breitling Navitimer B01", "Breitling", "Pilot", WatchCondition.GOOD, new BigDecimal("6900.00"), 3, WatchStatus.ACTIVE, "Pilot chronograph with slide rule bezel and in-house movement.", "https://images.unsplash.com/photo-1542496658-e33a6d0d50f6?auto=format&fit=crop&w=900&q=80", "AB0137241C1A1", 2021));
            watches.add(createWatch(seller2, "Seiko Prospex Diver 1968", "Seiko", "Dive", WatchCondition.NEW, new BigDecimal("1450.00"), 5, WatchStatus.ACTIVE, "Robust automatic diver with blue wave dial and modern finishing.", "https://images.unsplash.com/photo-1490367532201-b9bc1dc483f6?auto=format&fit=crop&w=900&q=80", "SPB381J1", 2024));
            watchRepository.saveAll(watches);

            Watch soldWatchOne = watches.get(0);
            Watch soldWatchTwo = watches.get(1);
            soldWatchOne.setStockQuantity(soldWatchOne.getStockQuantity() - 1);
            soldWatchTwo.setStockQuantity(soldWatchTwo.getStockQuantity() - 1);
            watchRepository.saveAll(List.of(soldWatchOne, soldWatchTwo));

            LocalDateTime completedAt = LocalDateTime.now().minusDays(12);
            Order completedOrderOne = Order.builder()
                .buyer(buyer1)
                .watch(soldWatchOne)
                .status(OrderStatus.COMPLETED)
                .totalAmount(soldWatchOne.getPrice())
                .trackingNumber("TIM-TRACK-1001")
                .createdAt(completedAt)
                .updatedAt(completedAt.plusDays(5))
                .build();
            Order completedOrderTwo = Order.builder()
                .buyer(buyer1)
                .watch(soldWatchTwo)
                .status(OrderStatus.COMPLETED)
                .totalAmount(soldWatchTwo.getPrice())
                .trackingNumber("TIM-TRACK-1002")
                .createdAt(completedAt.minusDays(2))
                .updatedAt(completedAt.plusDays(4))
                .build();
            orderRepository.saveAll(List.of(completedOrderOne, completedOrderTwo));

            Payment paymentOne = Payment.builder()
                .order(completedOrderOne)
                .buyer(buyer1)
                .amount(completedOrderOne.getTotalAmount())
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.COMPLETED)
                .transactionRef("BOOTSTRAP-" + java.util.UUID.randomUUID())
                .paidAt(completedAt.plusHours(2))
                .createdAt(completedAt.plusHours(2))
                .build();
            Payment paymentTwo = Payment.builder()
                .order(completedOrderTwo)
                .buyer(buyer1)
                .amount(completedOrderTwo.getTotalAmount())
                .method(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.COMPLETED)
                .transactionRef("BOOTSTRAP-" + java.util.UUID.randomUUID())
                .paidAt(completedAt.plusDays(1))
                .createdAt(completedAt.plusDays(1))
                .build();
            paymentRepository.saveAll(List.of(paymentOne, paymentTwo));

            Review review = Review.builder()
                .buyer(buyer1)
                .watch(soldWatchOne)
                .order(completedOrderOne)
                .rating(5)
                .comment("Absolutely stunning condition and fast delivery. A perfect first Timeless purchase.")
                .createdAt(LocalDateTime.now().minusDays(6))
                .build();
            reviewRepository.save(review);
        };
    }

    private UserAccount createUser(String email, String rawPassword, String fullName, Role role, String phone, String address) {
        return UserAccount.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .fullName(fullName)
            .phone(phone)
            .address(address)
            .role(role)
            .enabled(true)
            .emailVerified(true)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private Watch createWatch(
        UserAccount seller,
        String name,
        String brand,
        String category,
        WatchCondition condition,
        BigDecimal price,
        Integer stockQuantity,
        WatchStatus status,
        String description,
        String imageUrl,
        String referenceNumber,
        Integer year
    ) {
        LocalDateTime now = LocalDateTime.now();
        return Watch.builder()
            .seller(seller)
            .name(name)
            .brand(brand)
            .category(category)
            .condition(condition)
            .description(description)
            .price(price)
            .stockQuantity(stockQuantity)
            .status(status)
            .imageUrl(imageUrl)
            .referenceNumber(referenceNumber)
            .year(year)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
