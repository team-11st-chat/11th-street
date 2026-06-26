package com.elevenst.realtimechat.domain.message.dto;

import java.util.List;

public record ChatMessageHistoryResponse(
        List<ChatMessageResponse> content,
        Long nextCursor,
        boolean hasNext
) {

    public static ChatMessageHistoryResponse of(List<ChatMessageResponse> content, boolean hasNext) {
        Long nextCursor = content.isEmpty() ? null : content.get(content.size() - 1).id();
        return new ChatMessageHistoryResponse(content, nextCursor, hasNext);
    }
}
