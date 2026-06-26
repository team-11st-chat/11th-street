package com.elevenst.realtimechat.domain.order.service;

import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderRequest;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderResponse;
import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrder;
import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrderStatus;
import com.elevenst.realtimechat.domain.order.repository.TimeSaleOrderRepository;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSale;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStatus;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStock;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleRepository;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleStockRepository;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimeSaleOrderService {

    private final TimeSaleOrderRepository timeSaleOrderRepository;
    private final MemberRepository memberRepository;
    private final TimeSaleRepository timeSaleRepository;
    private final TimeSaleStockRepository timeSaleStockRepository;

    @Transactional
    public TimeSaleOrderResponse orderTimeSale(Long memberId, Long timeSaleId, String requestId, TimeSaleOrderRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST, "회원 정보가 없습니다."));

        TimeSale timeSale = timeSaleRepository.findById(timeSaleId)
                .orElseThrow(() -> new TimeSaleException(TimeSaleErrorCode.TIME_SALE_NOT_FOUND));

        timeSale.updateStatus(now);
        if (timeSale.getStatus() != TimeSaleStatus.ONGOING) {
            throw new TimeSaleException(TimeSaleErrorCode.TIME_SALE_001);
        }

        if (timeSaleOrderRepository.existsByMemberIdAndTimeSaleIdAndStatus(
                memberId, timeSaleId, TimeSaleOrderStatus.COMPLETED)) {
            throw new TimeSaleException(TimeSaleErrorCode.TIME_SALE_003);
        }

        TimeSaleStock timeSaleStock = timeSaleStockRepository.findByTimeSaleId(timeSaleId)
                .orElseThrow(() -> new TimeSaleException(TimeSaleErrorCode.TIME_SALE_NOT_FOUND));

        if (timeSaleStock.getRemainingQuantity() < request.quantity() || timeSale.getProduct().getStockQuantity() < request.quantity()) {
            throw new TimeSaleException(TimeSaleErrorCode.TIME_SALE_002);
        }

        timeSaleStock.decrease(request.quantity());
        timeSale.getProduct().decreaseStock(request.quantity());

        TimeSaleOrder order = new TimeSaleOrder(
                member,
                timeSale.getProduct().getId(),
                timeSale.getId(),
                requestId,
                timeSale.getProduct().getName(),
                timeSale.getOriginalPrice(),
                timeSale.getSalePrice(),
                request.quantity(),
                TimeSaleOrderStatus.COMPLETED,
                null,
                now
        );
        timeSaleOrderRepository.save(order);

        return TimeSaleOrderResponse.from(order);
    }
}
