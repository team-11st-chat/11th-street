package com.elevenst.realtimechat.domain.message.dto;

import java.math.BigDecimal;

public record ChatMessageProductResponse(
        Long id,
        String name,
        BigDecimal price
) {
}
