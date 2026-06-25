package com.elevenst.realtimechat.global.health;

import com.elevenst.realtimechat.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public ApiResponse<HealthCheckResponse> healthCheck() {
        return ApiResponse.success("헬스 체크 성공", HealthCheckResponse.up());
    }
}
