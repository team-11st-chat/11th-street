package com.elevenst.realtimechat.domain.promotion.exception;

import com.elevenst.realtimechat.global.exception.BusinessException;

public class CouponException extends BusinessException {

    public CouponException(CouponErrorCode errorCode) {
        super(errorCode);
    }
}
