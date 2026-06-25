package com.elevenst.realtimechat.domain.promotion.entity;

import com.elevenst.realtimechat.domain.promotion.exception.CouponErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.CouponException;
import com.elevenst.realtimechat.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false)
    private Long discountValue;

    @Column
    private Long maxDiscountAmount;

    @Column(nullable = false)
    private LocalDateTime issueStartsAt;

    @Column(nullable = false)
    private LocalDateTime issueEndsAt;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponPolicyStatus status;

    public CouponPolicy(String name, DiscountType discountType, Long discountValue, Long maxDiscountAmount,
                        LocalDateTime issueStartsAt, LocalDateTime issueEndsAt, int totalQuantity) {
        validateName(name);
        validateDiscountType(discountType);
        validateDiscountValue(discountType, discountValue);
        validateMaxDiscountAmount(maxDiscountAmount);
        validatePeriod(issueStartsAt, issueEndsAt);
        validateTotalQuantity(totalQuantity);

        this.name = name.trim();
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.issueStartsAt = issueStartsAt;
        this.issueEndsAt = issueEndsAt;
        this.totalQuantity = totalQuantity;
        // 생성 시 remaining_quantity 도 total_quantity 와 같은 값으로 초기화 (API-Spec 8.1)
        this.remainingQuantity = totalQuantity;
        this.status = calculateStatus(LocalDateTime.now());
    }

    /**
     * 영속성 컨텍스트에 로드되는 시점에 실시간 상태를 동기화한다.
     * 컬럼에 저장된 상태가 발급 기간 경과 등으로 실제 상태와 어긋나는 것을 방지한다.
     */
    @PostLoad
    private void syncStatusOnLoad() {
        updateStatus(LocalDateTime.now());
    }

    /**
     * 발급 기간과 잔여 수량으로 현재 상태를 재계산한다. (API-Spec 8.1)
     */
    public void updateStatus(LocalDateTime now) {
        this.status = calculateStatus(now);
    }

    /**
     * 선착순 발급을 시도한다. 발급 가능 여부 검증과 잔여 수량 차감을 한 흐름으로 처리한다.
     * 호출 측(서비스)에서 분산 락으로 보호된 상태에서 호출하는 것을 전제로 한다.
     */
    public void issue(LocalDateTime now) {
        if (!isWithinIssuePeriod(now)) {
            throw new CouponException(CouponErrorCode.COUPON_001);
        }
        if (remainingQuantity <= 0) {
            throw new CouponException(CouponErrorCode.COUPON_002);
        }
        this.remainingQuantity -= 1;
        updateStatus(now);
    }

    private boolean isWithinIssuePeriod(LocalDateTime now) {
        return !now.isBefore(issueStartsAt) && now.isBefore(issueEndsAt);
    }

    private CouponPolicyStatus calculateStatus(LocalDateTime now) {
        if (now.isBefore(issueStartsAt)) {
            return CouponPolicyStatus.SCHEDULED;
        }
        if (!now.isBefore(issueEndsAt) || remainingQuantity <= 0) {
            return CouponPolicyStatus.ENDED;
        }
        return CouponPolicyStatus.ACTIVE;
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isBlank() || name.trim().length() > 100) {
            throw new CouponException(CouponErrorCode.INVALID_COUPON_NAME);
        }
    }

    private static void validateDiscountType(DiscountType discountType) {
        if (discountType == null) {
            throw new CouponException(CouponErrorCode.INVALID_DISCOUNT_TYPE);
        }
    }

    private static void validateDiscountValue(DiscountType discountType, Long discountValue) {
        if (discountValue == null || discountValue <= 0) {
            throw new CouponException(CouponErrorCode.INVALID_DISCOUNT_VALUE);
        }
        if (discountType == DiscountType.PERCENTAGE && discountValue > 100) {
            throw new CouponException(CouponErrorCode.INVALID_DISCOUNT_VALUE);
        }
    }

    private static void validateMaxDiscountAmount(Long maxDiscountAmount) {
        if (maxDiscountAmount != null && maxDiscountAmount <= 0) {
            throw new CouponException(CouponErrorCode.INVALID_MAX_DISCOUNT_AMOUNT);
        }
    }

    private static void validatePeriod(LocalDateTime issueStartsAt, LocalDateTime issueEndsAt) {
        if (issueStartsAt == null || issueEndsAt == null || !issueEndsAt.isAfter(issueStartsAt)) {
            throw new CouponException(CouponErrorCode.INVALID_ISSUE_PERIOD);
        }
    }

    private static void validateTotalQuantity(int totalQuantity) {
        if (totalQuantity < 1) {
            throw new CouponException(CouponErrorCode.INVALID_TOTAL_QUANTITY);
        }
    }
}
