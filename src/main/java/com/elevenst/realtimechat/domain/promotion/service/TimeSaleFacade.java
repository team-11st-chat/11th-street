package com.elevenst.realtimechat.domain.promotion.service;

import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.service.ProductService;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleCreateRequest;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleResponse;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleUpdateRequest;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TimeSaleFacade {

    private final ProductService productService;
    private final TimeSaleService timeSaleService;

    @Transactional
    public TimeSaleResponse createTimeSale(Long sellerId, TimeSaleCreateRequest request) {
        Product product = productService.getProductEntity(request.productId());
        if (!product.getSellerId().equals(sellerId)) {
            throw new TimeSaleException(TimeSaleErrorCode.UNAUTHORIZED_OWNER);
        }
        return timeSaleService.createTimeSale(product, request);
    }

    @Transactional
    public TimeSaleResponse updateTimeSale(Long sellerId, Long timeSaleId, TimeSaleUpdateRequest request) {
        return timeSaleService.updateTimeSale(sellerId, timeSaleId, request);
    }

    public TimeSaleResponse getTimeSale(Long timeSaleId) {
        return timeSaleService.getTimeSale(timeSaleId);
    }

    public Page<TimeSaleResponse> getTimeSales(Pageable pageable) {
        return timeSaleService.getTimeSales(pageable);
    }
}
