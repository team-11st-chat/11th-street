package com.elevenst.realtimechat.domain.order.service;

import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.service.MemberQueryService;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderRequest;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderResponse;
import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrder;
import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrderStatus;
import com.elevenst.realtimechat.domain.order.repository.TimeSaleOrderRepository;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.domain.promotion.service.TimeSalePurchaseService;
import com.elevenst.realtimechat.domain.promotion.service.TimeSalePurchaseSnapshot;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimeSaleOrderService {

    private final TimeSaleOrderRepository timeSaleOrderRepository;
    private final MemberQueryService memberQueryService;
    private final TimeSalePurchaseService timeSalePurchaseService;

    @Transactional
    public TimeSaleOrderResponse orderTimeSale(Long memberId, Long timeSaleId, String requestId, TimeSaleOrderRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Member member = memberQueryService.getMemberOrThrow(memberId);
        timeSalePurchaseService.validatePurchasable(timeSaleId, now);

        if (timeSaleOrderRepository.existsByMemberIdAndTimeSaleIdAndStatus(
                memberId, timeSaleId, TimeSaleOrderStatus.COMPLETED)) {
            throw new TimeSaleException(TimeSaleErrorCode.TIME_SALE_003);
        }

        TimeSalePurchaseSnapshot timeSale = timeSalePurchaseService.purchase(timeSaleId, request.quantity(), now);

        TimeSaleOrder order = new TimeSaleOrder(
                member,
                timeSale.productId(),
                timeSale.timeSaleId(),
                requestId,
                timeSale.productName(),
                timeSale.originalPrice(),
                timeSale.salePrice(),
                request.quantity(),
                TimeSaleOrderStatus.COMPLETED,
                null,
                now
        );
        timeSaleOrderRepository.save(order);

        return TimeSaleOrderResponse.from(order);
    }
}
