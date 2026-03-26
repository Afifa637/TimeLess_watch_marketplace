package com.timeless.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String buyerName;
    private BigDecimal amount;
    private String method;
    private String status;
    private String transactionRef;
    private String paymentAccountRef;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
