package com.timeless.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentRequest {

    @NotNull
    private Long orderId;

    @NotBlank
    private String method;

    /** Phone number / card number / account ref for the selected payment method */
    private String paymentAccountRef;
}
