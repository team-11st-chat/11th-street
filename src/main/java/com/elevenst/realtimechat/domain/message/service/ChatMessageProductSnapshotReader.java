package com.elevenst.realtimechat.domain.message.service;

public interface ChatMessageProductSnapshotReader {

    ChatMessageProductSnapshot getSnapshot(Long productId);
}
