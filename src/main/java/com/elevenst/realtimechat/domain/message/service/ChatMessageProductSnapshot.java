package com.elevenst.realtimechat.domain.message.service;

import java.math.BigDecimal;

public record ChatMessageProductSnapshot(
        Long id,
        String name,
        BigDecimal price
) {
}
