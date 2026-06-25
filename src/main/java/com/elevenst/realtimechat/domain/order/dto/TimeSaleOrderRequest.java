package com.elevenst.realtimechat.domain.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TimeSaleOrderRequest(
        @NotNull @Min(1) @Max(1) Integer quantity
) {
}
