package com.elevenst.realtimechat.global.security;

import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증되지 않은 요청이 보호 경로에 접근할 때 401 로 거부한다.
 * 인증 필터 단계의 거부(미인증·만료·위조 토큰)는 SecurityContext 가 비어 있는 상태로 이 진입점에 도달한다.
 *
 * <p>응답 포맷을 일원화하기 위해 직접 직렬화하지 않고 {@link HandlerExceptionResolver} 에 위임해
 * {@code GlobalExceptionHandler} 가 공통 응답({@code ApiResponse})으로 변환하도록 한다.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public RestAuthenticationEntryPoint(HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) {
        handlerExceptionResolver.resolveException(
                request, response, null, new BusinessException(CommonErrorCode.UNAUTHORIZED));
    }
}
