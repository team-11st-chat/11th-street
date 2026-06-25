package com.elevenst.realtimechat.domain.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elevenst.realtimechat.domain.product.dto.ProductCreateRequest;
import com.elevenst.realtimechat.domain.product.dto.ProductResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductUpdateRequest;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import com.elevenst.realtimechat.domain.product.exception.ProductErrorCode;
import com.elevenst.realtimechat.domain.product.exception.ProductException;
import com.elevenst.realtimechat.domain.product.service.ProductService;
import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.global.security.AuthenticatedMember;
import com.elevenst.realtimechat.global.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

class ProductControllerTest {

    private MockMvc mockMvc;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(AuthenticatedMember.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new AuthenticatedMember(1L, MemberRole.SELLER);
                    }
                })
                .build();
    }

    @Test
    void createProduct_returnsCreatedProduct() throws Exception {
        when(productService.createProduct(eq(1L), any(ProductCreateRequest.class)))
                .thenReturn(new ProductResponse(1001L, 1L, 11L, "무선 이어폰", new BigDecimal("89000"), 500, SaleStatus.ON_SALE));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "무선 이어폰",
                                  "categoryId": 11,
                                  "price": 89000,
                                  "stockQuantity": 500
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("상품이 등록되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1001L))
                .andExpect(jsonPath("$.data.saleStatus").value("ON_SALE"));
    }

    @Test
    void updateProduct_returnsUpdatedProduct() throws Exception {
        when(productService.updateProduct(eq(1L), eq(1001L), any(ProductUpdateRequest.class)))
                .thenReturn(new ProductResponse(1001L, 1L, 11L, "무선 이어폰 Pro", new BigDecimal("99000"), 300, SaleStatus.ON_SALE));

        mockMvc.perform(patch("/api/v1/products/{productId}", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "무선 이어폰 Pro",
                                  "price": 99000,
                                  "stockQuantity": 300,
                                  "saleStatus": "ON_SALE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품이 수정되었습니다."))
                .andExpect(jsonPath("$.data.name").value("무선 이어폰 Pro"));
    }

    @Test
    void createProduct_rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "categoryId": 11,
                                  "price": 0,
                                  "stockQuantity": -1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_rejectsMissingStockQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "무선 이어폰",
                                  "categoryId": 11,
                                  "price": 89000
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct_returnsNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.updateProduct(eq(1L), eq(999L), any(ProductUpdateRequest.class)))
                .thenThrow(new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/products/{productId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "무선 이어폰 Pro"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
    }

    @Test
    void updateProduct_rejectsBlankName() throws Exception {
        mockMvc.perform(patch("/api/v1/products/{productId}", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
