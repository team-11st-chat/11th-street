package com.elevenst.realtimechat.domain.order.repository;

import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSaleOrderRepository extends JpaRepository<TimeSaleOrder, Long> {
    boolean existsByCompletedMemberIdAndTimeSaleId(Long completedMemberId, Long timeSaleId);
}
