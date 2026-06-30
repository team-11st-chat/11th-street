package com.elevenst.realtimechat.domain.promotion.service;

import com.elevenst.realtimechat.domain.promotion.entity.TimeSale;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStatus;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStock;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleRepository;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleStockRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimeSalePurchaseService {

    private final TimeSaleRepository timeSaleRepository;
    private final TimeSaleStockRepository timeSaleStockRepository;

    // 영속성 컨텍스트 안에서 엔티티 변경이 flush 되어야 하므로, 반드시 상위 트랜잭션 안에서만 호출되도록 강제한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public TimeSale validatePurchasable(Long timeSaleId, LocalDateTime now) {
        TimeSale timeSale = getTimeSale(timeSaleId);
        validateOngoing(timeSale, now);
        return timeSale;
    }

    // 재고/상품 차감이 영속 컨텍스트 밖에서 일어나 누락되는 것을 막기 위해 MANDATORY 로 트랜잭션을 강제한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public TimeSalePurchaseSnapshot purchase(TimeSale timeSale, int quantity, LocalDateTime now) {
        validateOngoing(timeSale, now);

        TimeSaleStock timeSaleStock = timeSaleStockRepository.findByTimeSaleId(timeSale.getId())
                .orElseThrow(() -> new TimeSaleException(TimeSaleErrorCode.TIME_SALE_NOT_FOUND));

        if (timeSaleStock.getRemainingQuantity() < quantity || timeSale.getProduct().getStockQuantity() < quantity) {
            throw new TimeSaleException(TimeSaleErrorCode.TIME_SALE_002);
        }

        timeSaleStock.decrease(quantity);
        timeSale.getProduct().decreaseStock(quantity);

        return new TimeSalePurchaseSnapshot(
                timeSale.getProduct().getId(),
                timeSale.getId(),
                timeSale.getProduct().getName(),
                timeSale.getOriginalPrice(),
                timeSale.getSalePrice()
        );
    }

    private TimeSale getTimeSale(Long timeSaleId) {
        return timeSaleRepository.findById(timeSaleId)
                .orElseThrow(() -> new TimeSaleException(TimeSaleErrorCode.TIME_SALE_NOT_FOUND));
    }

    private void validateOngoing(TimeSale timeSale, LocalDateTime now) {
        timeSale.updateStatus(now);
        if (timeSale.getStatus() != TimeSaleStatus.ONGOING) {
            throw new TimeSaleException(TimeSaleErrorCode.TIME_SALE_001);
        }
    }
}
