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
public class CartItemResponse {

    private Long id;
    private Long watchId;
    private String watchName;
    private String watchBrand;
    private String watchImageUrl;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
