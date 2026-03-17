package com.timeless.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchCreateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String brand;

    @NotBlank
    private String category;

    @NotBlank
    private String condition;

    private String description;

    @NotNull
    @Positive
    private BigDecimal price;

    @Builder.Default
    private Integer stockQuantity = 1;

    private String imageUrl;

    private String referenceNumber;

    private Integer year;
}
