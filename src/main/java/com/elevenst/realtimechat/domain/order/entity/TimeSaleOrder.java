package com.elevenst.realtimechat.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeSaleOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long timeSaleId;

    @Column(nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPriceSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salePriceSnapshot;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeSaleOrderStatus status;

    @Column(length = 255)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    @Column(insertable = false, updatable = false)
    private Long completedMemberId;

    public TimeSaleOrder(Long memberId, Long productId, Long timeSaleId, String requestId,
                         String productNameSnapshot, BigDecimal originalPriceSnapshot, BigDecimal salePriceSnapshot,
                         int quantity, TimeSaleOrderStatus status, String failureReason, LocalDateTime orderedAt) {
        this.memberId = memberId;
        this.productId = productId;
        this.timeSaleId = timeSaleId;
        this.requestId = requestId;
        this.productNameSnapshot = productNameSnapshot;
        this.originalPriceSnapshot = originalPriceSnapshot;
        this.salePriceSnapshot = salePriceSnapshot;
        this.quantity = quantity;
        this.status = status;
        this.failureReason = failureReason;
        this.orderedAt = orderedAt;
    }
}
