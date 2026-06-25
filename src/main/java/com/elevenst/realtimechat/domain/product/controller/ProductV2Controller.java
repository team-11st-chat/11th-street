package com.elevenst.realtimechat.domain.product.controller;

import com.elevenst.realtimechat.domain.product.dto.ProductPageResponse;
import com.elevenst.realtimechat.domain.product.service.ProductService;
import com.elevenst.realtimechat.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/products")
public class ProductV2Controller {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<ProductPageResponse> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(name = "Request-Guest-ID", required = false) String guestId
    ) {
        return ApiResponse.success(
                "조회 성공",
                productService.searchProductsV2(keyword, categoryId, page, size, guestId)
        );
    }
}
