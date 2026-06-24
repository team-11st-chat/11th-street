package com.elevenst.realtimechat.global.exception;

import lombok.Getter;

/**
 * STUB: 공통 예외 타입은 플랫폼 공통(global) 작업 단위 소유로, 이슈 #21 범위 밖이다.
 * TODO(실제 구현 교체 지점): 플랫폼 공통의 정식 예외 계층으로 교체.
 */
@Getter
public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
