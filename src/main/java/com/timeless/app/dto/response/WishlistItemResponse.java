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
public class WishlistItemResponse {

    private String id;
    private Long watchId;
    private String watchName;
    private String watchBrand;
    private String watchImageUrl;
    private BigDecimal price;
    private String watchStatus;
    private LocalDateTime addedAt;
}
