package com.elevenst.realtimechat.domain.order.controller;

import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderRequest;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderResponse;
import com.elevenst.realtimechat.domain.order.service.TimeSaleOrderFacade;
import com.elevenst.realtimechat.global.response.ApiResponse;
import com.elevenst.realtimechat.global.security.AuthenticatedMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private final TimeSaleOrderFacade timeSaleOrderFacade;

    @PostMapping("/{timeSaleId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TimeSaleOrderResponse> orderTimeSale(
            @PathVariable Long timeSaleId,
            @RequestHeader(value = "Request-ID", required = true) String requestId,
            @Valid @RequestBody TimeSaleOrderRequest request,
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.success(
                "주문이 성공적으로 처리되었습니다.",
                timeSaleOrderFacade.orderTimeSale(member.memberId(), timeSaleId, requestId, request)
        );
    }
}
