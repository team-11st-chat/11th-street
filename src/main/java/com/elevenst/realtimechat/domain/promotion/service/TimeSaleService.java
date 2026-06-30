package com.elevenst.realtimechat.domain.promotion.service;

import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleCreateRequest;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleResponse;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleUpdateRequest;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSale;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStock;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleRepository;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleStockRepository;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeSaleService {

    private final TimeSaleRepository timeSaleRepository;
    private final TimeSaleStockRepository timeSaleStockRepository;

    @Transactional
    public TimeSaleResponse createTimeSale(Product product, TimeSaleCreateRequest request) {
        TimeSale timeSale = new TimeSale(
                product,
                request.salePrice(),
                request.startedAt(),
                request.endedAt()
        );
        timeSaleRepository.save(timeSale);

        TimeSaleStock stock = new TimeSaleStock(timeSale, request.initialQuantity());
        timeSaleStockRepository.save(stock);

        return TimeSaleResponse.of(timeSale, stock.getRemainingQuantity());
    }

    @Transactional
    public TimeSaleResponse updateTimeSale(Long sellerId, Long timeSaleId, TimeSaleUpdateRequest request) {
        TimeSale timeSale = timeSaleRepository.findById(timeSaleId)
                .orElseThrow(() -> new TimeSaleException(TimeSaleErrorCode.TIME_SALE_NOT_FOUND));

        if (!timeSale.getProduct().getSellerId().equals(sellerId)) {
            throw new TimeSaleException(TimeSaleErrorCode.UNAUTHORIZED_OWNER);
        }

        LocalDateTime now = LocalDateTime.now();
        timeSale.update(request.salePrice(), request.startedAt(), request.endedAt(), now);

        TimeSaleStock stock = timeSaleStockRepository.findByTimeSaleId(timeSale.getId())
                .orElseThrow(() -> new TimeSaleException(TimeSaleErrorCode.TIME_SALE_NOT_FOUND));

        if (request.initialQuantity() != null) {
            if (now.isAfter(timeSale.getStartedAt())) {
                throw new TimeSaleException(TimeSaleErrorCode.MODIFICATION_NOT_ALLOWED);
            }
            stock.updateInitialQuantity(request.initialQuantity());
        }

        return TimeSaleResponse.of(timeSale, stock.getRemainingQuantity());
    }

    public TimeSaleResponse getTimeSale(Long timeSaleId) {
        TimeSale timeSale = timeSaleRepository.findById(timeSaleId)
                .orElseThrow(() -> new TimeSaleException(TimeSaleErrorCode.TIME_SALE_NOT_FOUND));
        timeSale.updateStatus(LocalDateTime.now());
        TimeSaleStock stock = timeSaleStockRepository.findByTimeSaleId(timeSale.getId())
                .orElseThrow(() -> new TimeSaleException(TimeSaleErrorCode.TIME_SALE_NOT_FOUND));

        return TimeSaleResponse.of(timeSale, stock.getRemainingQuantity());
    }

    public Page<TimeSaleResponse> getTimeSales(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return timeSaleRepository.findAll(pageable).map(timeSale -> {
            timeSale.updateStatus(now);
            TimeSaleStock stock = timeSaleStockRepository.findByTimeSaleId(timeSale.getId())
                    .orElseThrow(() -> new TimeSaleException(TimeSaleErrorCode.TIME_SALE_NOT_FOUND));
            return TimeSaleResponse.of(timeSale, stock.getRemainingQuantity());
        });
    }
}
