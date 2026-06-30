package com.elevenst.realtimechat.domain.category.exception;

import com.elevenst.realtimechat.global.exception.BusinessException;

public class CategoryException extends BusinessException {

    public CategoryException(CategoryErrorCode errorCode) {
        super(errorCode);
    }
}
