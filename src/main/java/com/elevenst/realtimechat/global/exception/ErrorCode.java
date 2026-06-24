package com.elevenst.realtimechat.global.exception;

import org.springframework.http.HttpStatus;

/**
 * STUB: 공통 에러 코드 모델은 플랫폼 공통(global) 작업 단위 소유로, 이슈 #21 범위 밖이다.
 * 도메인 에러 코드(409/401 등)를 표현하기 위한 최소 임시 계약이다.
 * TODO(실제 구현 교체 지점): 플랫폼 공통의 정식 에러 코드 모델로 교체.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getMessage();
}
