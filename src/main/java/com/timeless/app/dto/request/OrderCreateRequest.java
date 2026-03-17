package com.timeless.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @NotNull
    private Long watchId;

    @Min(1)
    private Integer quantity = 1;
}