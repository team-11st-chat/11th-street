package com.elevenst.realtimechat.domain.promotion.entity;

import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeSaleStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_sale_id", nullable = false, unique = true)
    private TimeSale timeSale;

    @Column(nullable = false)
    private int initialQuantity;

    @Column(nullable = false)
    private int remainingQuantity;

    public TimeSaleStock(TimeSale timeSale, int initialQuantity) {
        if (initialQuantity < 1) {
            throw new TimeSaleException(TimeSaleErrorCode.INVALID_QUANTITY);
        }
        this.timeSale = timeSale;
        this.initialQuantity = initialQuantity;
        this.remainingQuantity = initialQuantity;
    }

    public void updateInitialQuantity(int newQuantity) {
        if (newQuantity < 1) {
            throw new TimeSaleException(TimeSaleErrorCode.INVALID_QUANTITY);
        }
        this.initialQuantity = newQuantity;
        this.remainingQuantity = newQuantity;
    }

    public void decrease(int quantity) {
        if (this.remainingQuantity < quantity) {
            throw new TimeSaleException(TimeSaleErrorCode.TIME_SALE_002);
        }
        this.remainingQuantity -= quantity;
    }
}
