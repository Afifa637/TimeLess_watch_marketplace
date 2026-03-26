package com.timeless.app.service;

import com.timeless.app.dto.request.ReviewRequest;
import com.timeless.app.dto.response.ReviewResponse;
import com.timeless.app.entity.Order;
import com.timeless.app.entity.OrderStatus;
import com.timeless.app.entity.Review;
import com.timeless.app.entity.Role;
import com.timeless.app.exception.BadRequestException;
import com.timeless.app.exception.ForbiddenException;
import com.timeless.app.exception.ResourceNotFoundException;
import com.timeless.app.repository.OrderRepository;
import com.timeless.app.repository.ReviewRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public ReviewResponse createReview(ReviewRequest req, Long buyerId) {
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!Objects.equals(order.getBuyer().getId(), buyerId)) {
            throw new ForbiddenException("You can only review your own orders");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Can only review after confirming receipt");
        }
        if (reviewRepository.existsByBuyerIdAndWatchId(buyerId, order.getWatch().getId())
                || reviewRepository.existsByBuyerIdAndOrderId(buyerId, req.getOrderId())) {
            throw new BadRequestException("Already reviewed");
        }

        Review review = Review.builder()
                .buyer(order.getBuyer())
                .watch(order.getWatch())
                .order(order)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();
        return toResponse(reviewRepository.save(review));
    }

    public List<ReviewResponse> getReviewsForWatch(Long watchId) {
        return reviewRepository.findByWatchId(watchId).stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteReview(Long reviewId, Long currentUserId, Role role) {
        if (role != Role.ADMIN) {
            throw new ForbiddenException("Only admins can delete reviews");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        reviewRepository.delete(review);
    }

    public boolean hasBuyerReviewedWatch(Long buyerId, Long watchId) {
        return reviewRepository.existsByBuyerIdAndWatchId(buyerId, watchId);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .buyerName(review.getBuyer().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
