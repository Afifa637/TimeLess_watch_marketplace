package com.timeless.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchResponse {

    private Long id;
    private Long sellerId;
    private String sellerName;
    private String name;
    private String brand;
    private String category;
    private String condition;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String status;
    private String imageUrl;
    private String referenceNumber;
    private Integer year;
    private double averageRating;
    private int reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
