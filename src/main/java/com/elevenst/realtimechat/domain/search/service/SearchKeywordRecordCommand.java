package com.elevenst.realtimechat.domain.search.service;

public record SearchKeywordRecordCommand(
        String keyword,
        Long memberId,
        String guestUuid,
        Long categoryId
) {

    public static SearchKeywordRecordCommand guest(String keyword, String guestUuid, Long categoryId) {
        return new SearchKeywordRecordCommand(keyword, null, guestUuid, categoryId);
    }

    public static SearchKeywordRecordCommand member(String keyword, Long memberId, Long categoryId) {
        return new SearchKeywordRecordCommand(keyword, memberId, null, categoryId);
    }
}
