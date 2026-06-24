package com.elevenst.realtimechat.domain.product.controller;

import com.elevenst.realtimechat.domain.product.dto.ProductCreateRequest;
import com.elevenst.realtimechat.domain.product.dto.ProductApiResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductUpdateRequest;
import com.elevenst.realtimechat.domain.product.service.ProductService;
import com.elevenst.realtimechat.domain.product.support.FakeSellerStub;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final FakeSellerStub fakeSellerStub;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ProductApiResponse.success(
                "상품이 등록되었습니다.",
                productService.createProduct(fakeSellerStub.getSellerId(), request)
        );
    }

    @PatchMapping("/{productId}")
    public ProductApiResponse<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ProductApiResponse.success(
                "상품이 수정되었습니다.",
                productService.updateProduct(fakeSellerStub.getSellerId(), productId, request)
        );
    }
}
