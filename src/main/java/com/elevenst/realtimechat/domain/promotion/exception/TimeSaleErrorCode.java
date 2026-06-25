package com.elevenst.realtimechat.domain.promotion.exception;

import com.elevenst.realtimechat.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TimeSaleErrorCode implements ErrorCode {
    TIME_SALE_001(HttpStatus.CONFLICT, "판매 기간 외 요청입니다."),
    TIME_SALE_002(HttpStatus.CONFLICT, "타임세일 한정 판매 수량이 모두 소진되었습니다."),
    TIME_SALE_003(HttpStatus.CONFLICT, "이미 구매했거나 중복 주문 요청입니다."),
    INVALID_DISCOUNT_RATE(HttpStatus.BAD_REQUEST, "타임세일 할인율은 정상가 대비 최소 5% 이상, 100% 미만이어야 합니다."),
    INVALID_SALE_PRICE(HttpStatus.BAD_REQUEST, "타임세일 특가는 정상가보다 낮아야 하며, 최소 100원 이상이어야 합니다."),
    INVALID_SALE_PERIOD(HttpStatus.BAD_REQUEST, "종료 시각은 시작 시각보다 이후여야 합니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "한정 판매 수량은 1개 이상이어야 합니다."),
    MODIFICATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "판매 시작 이후에는 수정할 수 없습니다."),
    EXTENSION_ONLY_ALLOWED(HttpStatus.BAD_REQUEST, "종료 시각 변경은 연장만 가능합니다."),
    TIME_SALE_NOT_FOUND(HttpStatus.NOT_FOUND, "타임세일 정보를 찾을 수 없습니다."),
    UNAUTHORIZED_OWNER(HttpStatus.FORBIDDEN, "해당 타임세일의 소유자가 아닙니다.");

    private final HttpStatus httpStatus;
    private final String message;

    TimeSaleErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
