package com.timeless.app.service;

import com.timeless.app.dto.request.WatchCreateRequest;
import com.timeless.app.dto.request.WatchUpdateRequest;
import com.timeless.app.dto.response.WatchCardResponse;
import com.timeless.app.dto.response.WatchResponse;
import com.timeless.app.entity.Order;
import com.timeless.app.entity.OrderStatus;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.entity.Watch;
import com.timeless.app.entity.WatchCondition;
import com.timeless.app.entity.WatchStatus;
import com.timeless.app.exception.BadRequestException;
import com.timeless.app.exception.ForbiddenException;
import com.timeless.app.exception.ResourceNotFoundException;
import com.timeless.app.repository.CartItemRepository;
import com.timeless.app.repository.OrderRepository;
import com.timeless.app.repository.PaymentRepository;
import com.timeless.app.repository.ReviewRepository;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.repository.WatchRepository;
import com.timeless.app.repository.WishlistItemRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchService {
    private final WatchRepository watchRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public WatchResponse createWatch(WatchCreateRequest req, Long sellerId) {
        UserAccount seller = userAccountRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        if (seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only sellers can create watch listings");
        }

        Watch watch = Watch.builder()
                .seller(seller)
                .name(req.getName().trim())
                .brand(req.getBrand().trim())
                .category(req.getCategory().trim())
                .condition(parseCondition(req.getCondition()))
                .description(blankToNull(req.getDescription()))
                .price(req.getPrice())
                .stockQuantity(req.getStockQuantity() == null || req.getStockQuantity() < 1 ? 1 : req.getStockQuantity())
                .status(WatchStatus.PENDING_REVIEW)
                .imageUrl(blankToNull(req.getImageUrl()))
                .referenceNumber(blankToNull(req.getReferenceNumber()))
                .year(req.getYear())
                .build();
        return toResponse(watchRepository.save(watch));
    }

    public Page<WatchResponse> getActiveWatches(
            List<String> brands,
            List<String> categories,
            List<String> conditions,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search,
            Pageable pageable
    ) {
        List<String> resolvedBrands = normalizeList(brands);
        List<String> resolvedCategories = normalizeList(categories);
        List<WatchCondition> resolvedConditions = parseConditions(conditions);
        String normalizedSearch = normalizeSearch(search);

        BigDecimal dbMinPrice = watchRepository.findMinActivePrice();
        BigDecimal dbMaxPrice = watchRepository.findMaxActivePrice();

        BigDecimal tempMinPrice = minPrice;
        BigDecimal tempMaxPrice = maxPrice;

        if (dbMinPrice != null && dbMaxPrice != null) {
            if (tempMinPrice == null || tempMinPrice.compareTo(dbMinPrice) < 0) {
                tempMinPrice = dbMinPrice;
            }
            if (tempMaxPrice == null || tempMaxPrice.compareTo(dbMaxPrice) > 0) {
                tempMaxPrice = dbMaxPrice;
            }
            if (tempMinPrice.compareTo(tempMaxPrice) > 0) {
                tempMinPrice = dbMinPrice;
                tempMaxPrice = dbMaxPrice;
            }
        }

        final BigDecimal resolvedMinPrice = tempMinPrice;
        final BigDecimal resolvedMaxPrice = tempMaxPrice;

        Specification<Watch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), WatchStatus.ACTIVE));

            if (resolvedBrands != null) {
                predicates.add(root.get("brand").in(resolvedBrands));
            }
            if (resolvedCategories != null) {
                predicates.add(root.get("category").in(resolvedCategories));
            }
            if (resolvedConditions != null) {
                predicates.add(root.get("condition").in(resolvedConditions));
            }
            if (resolvedMinPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), resolvedMinPrice));
            }
            if (resolvedMaxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), resolvedMaxPrice));
            }
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), pattern),
                                cb.like(cb.lower(root.get("brand")), pattern)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Watch> page = watchRepository.findAll(spec, pageable);
        List<WatchResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    public BigDecimal getMinActivePrice() {
        BigDecimal min = watchRepository.findMinActivePrice();
        return min != null ? min : BigDecimal.ZERO;
    }

    public BigDecimal getMaxActivePrice() {
        BigDecimal max = watchRepository.findMaxActivePrice();
        return max != null ? max : BigDecimal.ZERO;
    }

    public List<WatchCardResponse> getFeaturedWatches(int limit) {
        return watchRepository.findByStatus(WatchStatus.ACTIVE, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(this::toCardResponse)
                .toList();
    }

    public WatchResponse getWatchById(Long id) {
        Watch watch = watchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Watch not found"));
        return toResponse(watch);
    }

    public List<WatchResponse> getWatchesBySeller(Long sellerId) {
        return watchRepository.findBySellerId(sellerId).stream()
                .sorted(Comparator.comparing(Watch::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WatchResponse updateWatch(Long watchId, WatchUpdateRequest req, Long currentUserId, Role currentRole) {
        Watch watch = watchRepository.findById(watchId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch not found"));

        validateWatchUpdateAccess(watch, currentUserId, currentRole);

        if (req.getName() != null) {
            watch.setName(req.getName().trim());
        }
        if (req.getBrand() != null) {
            watch.setBrand(req.getBrand().trim());
        }
        if (req.getCategory() != null) {
            watch.setCategory(req.getCategory().trim());
        }
        if (req.getCondition() != null) {
            watch.setCondition(parseCondition(req.getCondition()));
        }
        if (req.getDescription() != null) {
            watch.setDescription(blankToNull(req.getDescription()));
        }
        if (req.getPrice() != null) {
            if (req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Price must be positive");
            }
            watch.setPrice(req.getPrice());
        }
        if (req.getStockQuantity() != null) {
            if (req.getStockQuantity() < 0) {
                throw new BadRequestException("Stock quantity cannot be negative");
            }
            watch.setStockQuantity(req.getStockQuantity());
            if (req.getStockQuantity() == 0 && watch.getStatus() == WatchStatus.ACTIVE) {
                watch.setStatus(WatchStatus.INACTIVE);
            }
        }
        if (req.getImageUrl() != null) {
            watch.setImageUrl(blankToNull(req.getImageUrl()));
        }
        if (req.getReferenceNumber() != null) {
            watch.setReferenceNumber(blankToNull(req.getReferenceNumber()));
        }
        if (req.getYear() != null) {
            watch.setYear(req.getYear());
        }

        if (currentRole == Role.SELLER) {
            watch.setStatus(WatchStatus.PENDING_REVIEW);
        }
        return toResponse(watchRepository.save(watch));
    }

    @Transactional
    public void deleteWatch(Long watchId, Long currentUserId, Role currentRole) {
        Watch watch = watchRepository.findById(watchId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch not found"));

        if (currentRole == Role.SELLER) {
            if (!Objects.equals(watch.getSeller().getId(), currentUserId)) {
                throw new ForbiddenException("You can only delete your own watches");
            }
            boolean hasActiveOrPendingOrders = orderRepository.existsByWatchIdAndStatusIn(
                    watchId,
                    EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED)
            );
            if (hasActiveOrPendingOrders) {
                throw new BadRequestException("Cannot delete watch with active or pending orders");
            }
        } else if (currentRole != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to delete this watch");
        }

        reviewRepository.deleteByWatchId(watchId);
        List<Order> orders = orderRepository.findByWatchId(watchId);
        for (Order order : orders) {
            paymentRepository.findByOrderId(order.getId()).ifPresent(paymentRepository::delete);
        }
        orderRepository.deleteAll(orders);
        cartItemRepository.deleteByWatchId(watchId);
        wishlistItemRepository.deleteByWatchId(watchId);
        watchRepository.delete(watch);
    }

    @Transactional
    public WatchResponse approveWatch(Long watchId) {
        Watch watch = getManagedWatch(watchId);
        watch.setStatus(WatchStatus.ACTIVE);
        return toResponse(watchRepository.save(watch));
    }

    @Transactional
    public WatchResponse rejectWatch(Long watchId) {
        Watch watch = getManagedWatch(watchId);
        watch.setStatus(WatchStatus.REJECTED);
        return toResponse(watchRepository.save(watch));
    }

    @Transactional
    public WatchResponse deactivateWatch(Long watchId) {
        Watch watch = getManagedWatch(watchId);
        watch.setStatus(WatchStatus.INACTIVE);
        return toResponse(watchRepository.save(watch));
    }

    public List<WatchResponse> getAllWatchesForAdmin() {
        return watchRepository.findAll().stream()
                .sorted(Comparator.comparing(Watch::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    public List<WatchResponse> getPendingWatches() {
        return watchRepository.findByStatus(WatchStatus.PENDING_REVIEW, Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<WatchResponse> getWatchesByStatus(WatchStatus status) {
        return watchRepository.findByStatus(status, Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<WatchCardResponse> toCardResponses(List<WatchResponse> watches) {
        return watches.stream()
                .map(w -> WatchCardResponse.builder()
                        .id(w.getId())
                        .name(w.getName())
                        .brand(w.getBrand())
                        .category(w.getCategory())
                        .condition(w.getCondition())
                        .price(w.getPrice())
                        .status(w.getStatus())
                        .imageUrl(w.getImageUrl())
                        .sellerName(w.getSellerName())
                        .averageRating(w.getAverageRating())
                        .build())
                .collect(Collectors.toList());
    }

    private void validateWatchUpdateAccess(Watch watch, Long currentUserId, Role currentRole) {
        if (currentRole == Role.ADMIN) {
            return;
        }
        if (currentRole == Role.SELLER && Objects.equals(watch.getSeller().getId(), currentUserId)) {
            return;
        }
        throw new ForbiddenException("You do not have permission to update this watch");
    }

    private Watch getManagedWatch(Long watchId) {
        return watchRepository.findById(watchId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch not found"));
    }

    private WatchCondition parseCondition(String condition) {
        try {
            return WatchCondition.valueOf(condition.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid watch condition");
        }
    }

    private List<WatchCondition> parseConditions(List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }
        return conditions.stream().filter(Objects::nonNull).map(this::parseCondition).toList();
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .toList();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private WatchCardResponse toCardResponse(Watch watch) {
        double averageRating = calculateAverageRating(watch);
        return WatchCardResponse.builder()
                .id(watch.getId())
                .name(watch.getName())
                .brand(watch.getBrand())
                .category(watch.getCategory())
                .condition(watch.getCondition().name())
                .price(watch.getPrice())
                .status(watch.getStatus().name())
                .imageUrl(watch.getImageUrl())
                .sellerName(watch.getSeller().getFullName())
                .averageRating(averageRating)
                .build();
    }

    private WatchResponse toResponse(Watch watch) {
        double averageRating = calculateAverageRating(watch);
        int reviewCount = watch.getReviews() == null ? 0 : watch.getReviews().size();
        return WatchResponse.builder()
                .id(watch.getId())
                .sellerId(watch.getSeller().getId())
                .sellerName(watch.getSeller().getFullName())
                .name(watch.getName())
                .brand(watch.getBrand())
                .category(watch.getCategory())
                .condition(watch.getCondition().name())
                .description(watch.getDescription())
                .price(watch.getPrice())
                .stockQuantity(watch.getStockQuantity())
                .status(watch.getStatus().name())
                .imageUrl(watch.getImageUrl())
                .referenceNumber(watch.getReferenceNumber())
                .year(watch.getYear())
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .createdAt(watch.getCreatedAt())
                .updatedAt(watch.getUpdatedAt())
                .build();
    }

    private double calculateAverageRating(Watch watch) {
        if (watch.getReviews() == null || watch.getReviews().isEmpty()) {
            return 0.0;
        }
        return Math.round(watch.getReviews().stream().mapToInt(r -> r.getRating()).average().orElse(0.0) * 10.0) / 10.0;
    }
}
