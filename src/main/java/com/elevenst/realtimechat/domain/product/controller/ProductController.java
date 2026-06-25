package com.elevenst.realtimechat.domain.product.controller;

import com.elevenst.realtimechat.domain.product.dto.ProductCreateRequest;
import com.elevenst.realtimechat.domain.product.dto.ProductPageResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductUpdateRequest;
import com.elevenst.realtimechat.domain.product.service.ProductService;
import com.elevenst.realtimechat.global.security.AuthenticatedMember;
import com.elevenst.realtimechat.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

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
                productService.searchProducts(keyword, categoryId, page, size, guestId)
        );
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(
                "조회 성공",
                productService.getProduct(productId)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody ProductCreateRequest request
    ) {
        return ApiResponse.success(
                "상품이 등록되었습니다.",
                productService.createProduct(member.memberId(), request)
        );
    }

    @PatchMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ApiResponse.success(
                "상품이 수정되었습니다.",
                productService.updateProduct(member.memberId(), productId, request)
        );
    }
}
