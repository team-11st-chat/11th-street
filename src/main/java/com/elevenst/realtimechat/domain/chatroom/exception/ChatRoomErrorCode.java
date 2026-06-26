package com.elevenst.realtimechat.domain.chatroom.exception;

import com.elevenst.realtimechat.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatRoomErrorCode implements ErrorCode {
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Chat room not found."),
    INVALID_MEMBER(HttpStatus.BAD_REQUEST, "Member information is invalid."),
    INVALID_ROOM_TYPE(HttpStatus.BAD_REQUEST, "Chat room type is invalid."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found."),
    ACTIVE_CS_ROOM_EXISTS(HttpStatus.CONFLICT, "An active CS chat room already exists."),
    CS_ROOM_NOT_WAITING(HttpStatus.CONFLICT, "CS chat room is not waiting."),
    CS_ROOM_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "CS chat room is not in progress."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Chat room access is denied."),
    CS_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "CS admin role is required."),
    LOCK_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Chat room request is temporarily unavailable.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
