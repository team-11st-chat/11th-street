package com.elevenst.realtimechat.domain.product.exception;

import com.elevenst.realtimechat.global.exception.BusinessException;

public class ProductException extends BusinessException {

    public ProductException(ProductErrorCode errorCode) {
        super(errorCode);
    }
}
