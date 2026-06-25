package com.elevenst.realtimechat.domain.promotion.entity;

import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeSale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeSaleStatus status;

    public TimeSale(Product product, BigDecimal salePrice, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.product = product;
        this.originalPrice = product.getPrice();
        this.salePrice = salePrice;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.status = calculateStatus(LocalDateTime.now());

        validateSalePrice();
        validatePeriod();
    }

    private void validateSalePrice() {
        if (salePrice.compareTo(new BigDecimal("100")) < 0) {
            throw new TimeSaleException(TimeSaleErrorCode.INVALID_SALE_PRICE);
        }
        if (salePrice.compareTo(originalPrice) >= 0) {
            throw new TimeSaleException(TimeSaleErrorCode.INVALID_SALE_PRICE);
        }

        BigDecimal discountRate = originalPrice.subtract(salePrice)
                .divide(originalPrice, 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.DOWN);

        if (discountRate.compareTo(new BigDecimal("5")) < 0 || discountRate.compareTo(new BigDecimal("100")) >= 0) {
            throw new TimeSaleException(TimeSaleErrorCode.INVALID_DISCOUNT_RATE);
        }
    }

    private void validatePeriod() {
        if (!endedAt.isAfter(startedAt)) {
            throw new TimeSaleException(TimeSaleErrorCode.INVALID_SALE_PERIOD);
        }
    }

    public void update(BigDecimal salePrice, LocalDateTime startedAt, LocalDateTime endedAt, LocalDateTime now) {
        if (now.isAfter(this.startedAt)) {
            if (salePrice != null || startedAt != null) {
                throw new TimeSaleException(TimeSaleErrorCode.MODIFICATION_NOT_ALLOWED);
            }
            if (endedAt != null) {
                if (this.status == TimeSaleStatus.ENDED) {
                    throw new TimeSaleException(TimeSaleErrorCode.MODIFICATION_NOT_ALLOWED);
                }
                if (!endedAt.isAfter(this.endedAt)) {
                    throw new TimeSaleException(TimeSaleErrorCode.EXTENSION_ONLY_ALLOWED);
                }
                this.endedAt = endedAt;
            }
        } else {
            if (salePrice != null) this.salePrice = salePrice;
            if (startedAt != null) this.startedAt = startedAt;
            if (endedAt != null) this.endedAt = endedAt;

            validateSalePrice();
            validatePeriod();
        }
        updateStatus(now);
    }

    public void updateStatus(LocalDateTime now) {
        this.status = calculateStatus(now);
    }

    private TimeSaleStatus calculateStatus(LocalDateTime now) {
        if (now.isBefore(startedAt)) return TimeSaleStatus.SCHEDULED;
        if (!now.isBefore(endedAt)) return TimeSaleStatus.ENDED;
        return TimeSaleStatus.ONGOING;
    }
}
