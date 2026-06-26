package com.elevenst.realtimechat.domain.chatroom.exception;

import com.elevenst.realtimechat.global.exception.BusinessException;

public class ChatRoomException extends BusinessException {

    public ChatRoomException(ChatRoomErrorCode errorCode) {
        super(errorCode);
    }
}
