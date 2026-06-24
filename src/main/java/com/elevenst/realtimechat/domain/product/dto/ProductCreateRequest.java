package com.elevenst.realtimechat.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        Long categoryId,

        @NotNull
        @Positive
        BigDecimal price,

        @Min(0)
        int stockQuantity
) {
}
