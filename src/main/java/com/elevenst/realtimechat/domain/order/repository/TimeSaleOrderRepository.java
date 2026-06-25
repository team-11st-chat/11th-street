package com.elevenst.realtimechat.domain.order.repository;

import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrder;
import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSaleOrderRepository extends JpaRepository<TimeSaleOrder, Long> {
    boolean existsByMemberIdAndTimeSaleIdAndStatus(Long memberId, Long timeSaleId, TimeSaleOrderStatus status);
}
