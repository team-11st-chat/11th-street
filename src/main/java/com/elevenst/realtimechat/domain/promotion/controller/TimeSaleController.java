package com.elevenst.realtimechat.domain.promotion.controller;

import com.elevenst.realtimechat.domain.product.support.FakeSellerStub;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleCreateRequest;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleResponse;
import com.elevenst.realtimechat.domain.promotion.dto.TimeSaleUpdateRequest;
import com.elevenst.realtimechat.domain.promotion.service.TimeSaleService;
import com.elevenst.realtimechat.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/timesales")
public class TimeSaleController {

    private final TimeSaleService timeSaleService;
    private final FakeSellerStub fakeSellerStub;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TimeSaleResponse> createTimeSale(@Valid @RequestBody TimeSaleCreateRequest request) {
        return ApiResponse.success(
                "타임세일이 등록되었습니다.",
                timeSaleService.createTimeSale(fakeSellerStub.getSellerId(), request)
        );
    }

    @PatchMapping("/{timeSaleId}")
    public ApiResponse<TimeSaleResponse> updateTimeSale(
            @PathVariable Long timeSaleId,
            @Valid @RequestBody TimeSaleUpdateRequest request
    ) {
        return ApiResponse.success(
                "타임세일이 수정되었습니다.",
                timeSaleService.updateTimeSale(fakeSellerStub.getSellerId(), timeSaleId, request)
        );
    }

    @GetMapping("/{timeSaleId}")
    public ApiResponse<TimeSaleResponse> getTimeSale(@PathVariable Long timeSaleId) {
        return ApiResponse.success(
                "조회 성공",
                timeSaleService.getTimeSale(timeSaleId)
        );
    }

    @GetMapping
    public ApiResponse<Page<TimeSaleResponse>> getTimeSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(
                "조회 성공",
                timeSaleService.getTimeSales(pageable)
        );
    }
}
