package com.timeless.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CartCheckoutRequest {

    @NotBlank
    private String method;

    /** Phone number / card number / account reference for chosen payment method */
    private String paymentAccountRef;
}
