package com.timeless.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long buyerId;
    private String buyerName;
    private Long watchId;
    private String watchName;
    private String watchBrand;
    private String watchImageUrl;
    private String sellerName;
    private String status;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String trackingNumber;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
