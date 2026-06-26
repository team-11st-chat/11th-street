package com.elevenst.realtimechat.domain.message.exception;

import com.elevenst.realtimechat.global.exception.BusinessException;

public class ChatMessageException extends BusinessException {

    public ChatMessageException(ChatMessageErrorCode errorCode) {
        super(errorCode);
    }

    public ChatMessageException(ChatMessageErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
