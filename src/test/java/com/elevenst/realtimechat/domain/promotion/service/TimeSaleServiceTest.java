package com.elevenst.realtimechat.domain.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.elevenst.realtimechat.domain.category.entity.Category;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleResponse;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSale;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStock;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleRepository;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleStockRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TimeSaleServiceTest {

    @Mock
    private TimeSaleRepository timeSaleRepository;

    @Mock
    private TimeSaleStockRepository timeSaleStockRepository;

    private TimeSaleService timeSaleService;

    @BeforeEach
    void setUp() {
        timeSaleService = new TimeSaleService(timeSaleRepository, timeSaleStockRepository);
    }

    @Test
    void 타임세일_목록_조회는_재고를_단일_쿼리로_일괄_조회한다() {
        LocalDateTime now = LocalDateTime.now();
        TimeSale timeSale1 = createTimeSale(10L, createProduct(100L), now.minusMinutes(1), now.plusMinutes(1));
        TimeSale timeSale2 = createTimeSale(11L, createProduct(101L), now.minusMinutes(1), now.plusMinutes(1));
        Pageable pageable = PageRequest.of(0, 20);
        Page<TimeSale> page = new PageImpl<>(List.of(timeSale1, timeSale2), pageable, 2);
        given(timeSaleRepository.findAll(pageable)).willReturn(page);
        given(timeSaleStockRepository.findByTimeSaleIdIn(List.of(10L, 11L)))
                .willReturn(List.of(new TimeSaleStock(timeSale1, 5), new TimeSaleStock(timeSale2, 3)));

        Page<TimeSaleResponse> result = timeSaleService.getTimeSales(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(TimeSaleResponse::id, TimeSaleResponse::remainingQuantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, 5),
                        org.assertj.core.groups.Tuple.tuple(11L, 3)
                );
        verify(timeSaleStockRepository, times(1)).findByTimeSaleIdIn(List.of(10L, 11L));
        verify(timeSaleStockRepository, never()).findByTimeSaleId(org.mockito.ArgumentMatchers.anyLong());
    }

    private Product createProduct(Long productId) {
        Category root = Category.createRoot("Digital", 1);
        Category category = Category.createChild(root, "Audio", 1);
        Product product = Product.create(1L, category, "Time sale product", new BigDecimal("10000"), 10);
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }

    private TimeSale createTimeSale(Long timeSaleId, Product product, LocalDateTime startedAt, LocalDateTime endedAt) {
        TimeSale timeSale = new TimeSale(product, new BigDecimal("9000"), startedAt, endedAt);
        ReflectionTestUtils.setField(timeSale, "id", timeSaleId);
        return timeSale;
    }
}
