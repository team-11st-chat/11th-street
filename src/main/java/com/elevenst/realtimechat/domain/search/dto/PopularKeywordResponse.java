package com.elevenst.realtimechat.domain.search.dto;

import com.elevenst.realtimechat.domain.search.repository.SearchHistoryRepository.PopularKeywordRow;

public record PopularKeywordResponse(
        String keyword,
        long searchCount
) {

    public static PopularKeywordResponse from(PopularKeywordRow row) {
        return new PopularKeywordResponse(row.getKeyword(), row.getSearchCount());
    }
}
