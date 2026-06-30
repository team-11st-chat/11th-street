package com.elevenst.realtimechat.domain.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.service.ProductService;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleCreateRequest;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleResponse;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleUpdateRequest;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSale;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TimeSaleFacadeTest {

    @Mock
    private ProductService productService;

    @Mock
    private TimeSaleService timeSaleService;

    private TimeSaleFacade timeSaleFacade;

    @BeforeEach
    void setUp() {
        timeSaleFacade = new TimeSaleFacade(productService, timeSaleService);
    }

    @Test
    void 타임세일을_생성한다() {
        // given
        Long sellerId = 1L;
        Long productId = 10L;
        Product product = createProduct(sellerId, productId);
        TimeSaleCreateRequest request = new TimeSaleCreateRequest(
                productId,
                new BigDecimal("9000"),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                10
        );

        given(productService.getProductEntity(productId)).willReturn(product);
        TimeSaleResponse expectedResponse = mock(TimeSaleResponse.class);
        given(timeSaleService.createTimeSale(product, request)).willReturn(expectedResponse);

        // when
        TimeSaleResponse response = timeSaleFacade.createTimeSale(sellerId, request);

        // then
        assertThat(response).isEqualTo(expectedResponse);
        verify(productService).getProductEntity(productId);
        verify(timeSaleService).createTimeSale(product, request);
    }

    @Test
    void 타임세일_생성_시_판매자가_다르면_예외를_반환한다() {
        // given
        Long sellerId = 1L;
        Long anotherSellerId = 2L;
        Long productId = 10L;
        Product product = createProduct(anotherSellerId, productId);
        TimeSaleCreateRequest request = new TimeSaleCreateRequest(
                productId,
                new BigDecimal("9000"),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                10
        );

        given(productService.getProductEntity(productId)).willReturn(product);

        // when & then
        assertThatThrownBy(() -> timeSaleFacade.createTimeSale(sellerId, request))
                .isInstanceOf(TimeSaleException.class)
                .extracting(exception -> ((TimeSaleException) exception).getErrorCode())
                .isEqualTo(TimeSaleErrorCode.UNAUTHORIZED_OWNER);

        verifyNoInteractions(timeSaleService);
    }

    @Test
    void 타임세일을_수정한다() {
        // given
        Long sellerId = 1L;
        Long timeSaleId = 100L;
        TimeSaleUpdateRequest request = new TimeSaleUpdateRequest(
                new BigDecimal("8000"),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                20
        );
        TimeSaleResponse expectedResponse = mock(TimeSaleResponse.class);
        given(timeSaleService.updateTimeSale(sellerId, timeSaleId, request)).willReturn(expectedResponse);

        // when
        TimeSaleResponse response = timeSaleFacade.updateTimeSale(sellerId, timeSaleId, request);

        // then
        assertThat(response).isEqualTo(expectedResponse);
        verify(timeSaleService).updateTimeSale(sellerId, timeSaleId, request);
    }

    private Product createProduct(Long sellerId, Long productId) {
        Category root = Category.createRoot("Digital", 1);
        Category category = Category.createChild(root, "Audio", 1);
        Product product = Product.create(sellerId, category, "Time sale product", new BigDecimal("10000"), 100);
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }
}
