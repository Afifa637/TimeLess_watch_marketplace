package com.timeless.app.controller;

import com.timeless.app.dto.request.ReviewRequest;
import com.timeless.app.dto.response.ReviewResponse;
import com.timeless.app.security.UserPrincipal;
import com.timeless.app.service.ReviewService;
import com.timeless.app.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Watch reviews by buyers")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Create a review for a delivered or completed order")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request, user.getId()));
    }

    @GetMapping("/watch/{watchId}")
    @Operation(summary = "Get reviews for a watch")
    public ResponseEntity<List<ReviewResponse>> getReviewsForWatch(@PathVariable Long watchId) {
        return ResponseEntity.ok(reviewService.getReviewsForWatch(watchId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a review as admin")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        reviewService.deleteReview(id, user.getId(), user.getRole());
        return ResponseEntity.noContent().build();
    }
}
