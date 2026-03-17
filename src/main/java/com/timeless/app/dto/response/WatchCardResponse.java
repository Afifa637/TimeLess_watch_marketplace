package com.timeless.app.dto.response;

import java.math.BigDecimal;
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
public class WatchCardResponse {

    private Long id;
    private String name;
    private String brand;
    private String category;
    private String condition;
    private BigDecimal price;
    private String status;
    private String imageUrl;
    private String sellerName;
    private double averageRating;
}
