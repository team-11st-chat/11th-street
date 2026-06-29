package com.elevenst.realtimechat.domain.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSale;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStock;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleRepository;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleStockRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TimeSalePurchaseServiceTest {

    @Mock
    private TimeSaleRepository timeSaleRepository;

    @Mock
    private TimeSaleStockRepository timeSaleStockRepository;

    private TimeSalePurchaseService timeSalePurchaseService;

    @BeforeEach
    void setUp() {
        timeSalePurchaseService = new TimeSalePurchaseService(timeSaleRepository, timeSaleStockRepository);
    }

    @Test
    void 타임세일_구매가_가능하면_재고를_차감하고_주문_스냅샷을_반환한다() {
        LocalDateTime now = LocalDateTime.now();
        Product product = createProduct(100L, 10);
        TimeSale timeSale = createTimeSale(10L, product, now.minusMinutes(1), now.plusMinutes(1));
        TimeSaleStock stock = new TimeSaleStock(timeSale, 5);
        given(timeSaleRepository.findById(10L)).willReturn(Optional.of(timeSale));
        given(timeSaleStockRepository.findByTimeSaleId(10L)).willReturn(Optional.of(stock));

        TimeSalePurchaseSnapshot result = timeSalePurchaseService.purchase(10L, 3, now);

        assertThat(result.productId()).isEqualTo(100L);
        assertThat(result.timeSaleId()).isEqualTo(10L);
        assertThat(result.productName()).isEqualTo("Time sale product");
        assertThat(result.originalPrice()).isEqualByComparingTo("10000");
        assertThat(result.salePrice()).isEqualByComparingTo("9000");
        assertThat(stock.getRemainingQuantity()).isEqualTo(2);
        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void 타임세일이_존재하지_않으면_예외가_발생한다() {
        given(timeSaleRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> timeSalePurchaseService.purchase(10L, 1, LocalDateTime.now()))
                .isInstanceOf(TimeSaleException.class)
                .extracting(exception -> ((TimeSaleException) exception).getErrorCode())
                .isEqualTo(TimeSaleErrorCode.TIME_SALE_NOT_FOUND);
    }

    @Test
    void 타임세일이_진행중이_아니면_재고를_조회하지_않고_예외가_발생한다() {
        LocalDateTime now = LocalDateTime.now();
        TimeSale timeSale = createTimeSale(10L, createProduct(100L, 10), now.plusMinutes(1), now.plusMinutes(10));
        given(timeSaleRepository.findById(10L)).willReturn(Optional.of(timeSale));

        assertThatThrownBy(() -> timeSalePurchaseService.purchase(10L, 1, now))
                .isInstanceOf(TimeSaleException.class)
                .extracting(exception -> ((TimeSaleException) exception).getErrorCode())
                .isEqualTo(TimeSaleErrorCode.TIME_SALE_001);
        verify(timeSaleStockRepository, never()).findByTimeSaleId(10L);
    }

    @Test
    void 구매_가능_검증은_진행중인_타임세일이면_통과한다() {
        LocalDateTime now = LocalDateTime.now();
        TimeSale timeSale = createTimeSale(10L, createProduct(100L, 10), now.minusMinutes(1), now.plusMinutes(1));
        given(timeSaleRepository.findById(10L)).willReturn(Optional.of(timeSale));

        timeSalePurchaseService.validatePurchasable(10L, now);

        verify(timeSaleStockRepository, never()).findByTimeSaleId(10L);
    }

    @Test
    void 구매_가능_검증은_진행중이_아니면_예외가_발생한다() {
        LocalDateTime now = LocalDateTime.now();
        TimeSale timeSale = createTimeSale(10L, createProduct(100L, 10), now.plusMinutes(1), now.plusMinutes(10));
        given(timeSaleRepository.findById(10L)).willReturn(Optional.of(timeSale));

        assertThatThrownBy(() -> timeSalePurchaseService.validatePurchasable(10L, now))
                .isInstanceOf(TimeSaleException.class)
                .extracting(exception -> ((TimeSaleException) exception).getErrorCode())
                .isEqualTo(TimeSaleErrorCode.TIME_SALE_001);
        verify(timeSaleStockRepository, never()).findByTimeSaleId(10L);
    }

    @Test
    void 타임세일_재고가_부족하면_예외가_발생한다() {
        LocalDateTime now = LocalDateTime.now();
        Product product = createProduct(100L, 10);
        TimeSale timeSale = createTimeSale(10L, product, now.minusMinutes(1), now.plusMinutes(1));
        TimeSaleStock stock = new TimeSaleStock(timeSale, 1);
        given(timeSaleRepository.findById(10L)).willReturn(Optional.of(timeSale));
        given(timeSaleStockRepository.findByTimeSaleId(10L)).willReturn(Optional.of(stock));

        assertThatThrownBy(() -> timeSalePurchaseService.purchase(10L, 2, now))
                .isInstanceOf(TimeSaleException.class)
                .extracting(exception -> ((TimeSaleException) exception).getErrorCode())
                .isEqualTo(TimeSaleErrorCode.TIME_SALE_002);
        assertThat(stock.getRemainingQuantity()).isEqualTo(1);
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    private Product createProduct(Long productId, int stockQuantity) {
        Category root = Category.createRoot("Digital", 1);
        Category category = Category.createChild(root, "Audio", 1);
        Product product = Product.create(1L, category, "Time sale product", new BigDecimal("10000"), stockQuantity);
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }

    private TimeSale createTimeSale(Long timeSaleId, Product product, LocalDateTime startedAt, LocalDateTime endedAt) {
        TimeSale timeSale = new TimeSale(product, new BigDecimal("9000"), startedAt, endedAt);
        ReflectionTestUtils.setField(timeSale, "id", timeSaleId);
        return timeSale;
    }
}
