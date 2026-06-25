package com.elevenst.realtimechat.domain.order.dto;

import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrder;
import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TimeSaleOrderResponse(
        Long id,
        Long timeSaleId,
        Long productId,
        int quantity,
        BigDecimal salePriceSnapshot,
        TimeSaleOrderStatus status,
        LocalDateTime orderedAt
) {
    public static TimeSaleOrderResponse from(TimeSaleOrder order) {
        return new TimeSaleOrderResponse(
                order.getId(),
                order.getTimeSaleId(),
                order.getProductId(),
                order.getQuantity(),
                order.getSalePriceSnapshot(),
                order.getStatus(),
                order.getOrderedAt()
        );
    }
}
