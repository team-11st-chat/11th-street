package com.elevenst.realtimechat.domain.order.service;

import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderRequest;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderResponse;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import com.elevenst.realtimechat.global.support.IdempotencyManager;
import com.elevenst.realtimechat.global.support.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeSaleOrderFacade {

    private static final long REQUEST_ID_TTL_SECONDS = 10;
    private static final String TIME_SALE_LOCK_KEY_PREFIX = "lock:timesale:";

    private final TimeSaleOrderService timeSaleOrderService;
    private final LockManager lockManager;
    private final IdempotencyManager idempotencyManager;

    public TimeSaleOrderResponse orderTimeSale(Long memberId, Long timeSaleId, String requestId, TimeSaleOrderRequest request) {
        if (!idempotencyManager.checkAndSet(requestId, REQUEST_ID_TTL_SECONDS)) {
            throw new TimeSaleException(TimeSaleErrorCode.TIME_SALE_003);
        }

        String lockKey = TIME_SALE_LOCK_KEY_PREFIX + timeSaleId;
        boolean locked = lockManager.tryLock(lockKey);
        if (!locked) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, "일시적으로 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        }

        try {
            return timeSaleOrderService.orderTimeSale(memberId, timeSaleId, requestId, request);
        } finally {
            lockManager.unlock(lockKey);
        }
    }
}
