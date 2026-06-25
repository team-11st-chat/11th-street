package com.elevenst.realtimechat.domain.order.controller;

import com.elevenst.realtimechat.domain.member.support.FakeMemberStub;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderRequest;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderResponse;
import com.elevenst.realtimechat.domain.order.service.TimeSaleOrderService;
import com.elevenst.realtimechat.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/timesales")
public class TimeSaleOrderController {

    private final TimeSaleOrderService timeSaleOrderService;
    private final FakeMemberStub fakeMemberStub;

    @PostMapping("/{timeSaleId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TimeSaleOrderResponse> orderTimeSale(
            @PathVariable Long timeSaleId,
            @RequestHeader("Request-ID") String requestId,
            @Valid @RequestBody TimeSaleOrderRequest request
    ) {
        return ApiResponse.success(
                "주문이 완료되었습니다.",
                timeSaleOrderService.orderTimeSale(fakeMemberStub.getMemberId(), timeSaleId, requestId, request)
        );
    }
}
