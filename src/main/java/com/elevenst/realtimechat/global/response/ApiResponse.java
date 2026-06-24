package com.elevenst.realtimechat.global.response;

/**
 * STUB: 공통 응답 형식은 플랫폼 공통(global) 작업 단위 소유로, 이슈 #21 범위 밖이다.
 * 회원/인증 API 가 응답을 감싸기 위한 최소 임시 구현이다.
 *
 * TODO(실제 구현 교체 지점): 플랫폼 공통이 정의하는 정식 응답 규격으로 교체.
 */
public record ApiResponse<T>(String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("성공", data);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(message, null);
    }
}
