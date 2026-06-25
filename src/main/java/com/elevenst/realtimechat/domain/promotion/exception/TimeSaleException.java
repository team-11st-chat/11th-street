package com.elevenst.realtimechat.domain.promotion.exception;

import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.ErrorCode;

public class TimeSaleException extends BusinessException {
    public TimeSaleException(ErrorCode errorCode) {
        super(errorCode);
    }
}
