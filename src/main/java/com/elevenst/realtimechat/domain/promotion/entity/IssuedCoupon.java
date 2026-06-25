package com.elevenst.realtimechat.domain.promotion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "issued_coupon",
        // 이벤트 전체 기간 내 1인 1장 제한 (ERD: uq_issued_coupon_policy_member)
        uniqueConstraints = @UniqueConstraint(
                name = "uq_issued_coupon_policy_member",
                columnNames = {"coupon_policy_id", "member_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_policy_id", nullable = false)
    private Long couponPolicyId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssuedCouponStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    public IssuedCoupon(Long couponPolicyId, Long memberId, LocalDateTime issuedAt) {
        this.couponPolicyId = couponPolicyId;
        this.memberId = memberId;
        this.status = IssuedCouponStatus.ISSUED;
        this.issuedAt = issuedAt;
    }
}
