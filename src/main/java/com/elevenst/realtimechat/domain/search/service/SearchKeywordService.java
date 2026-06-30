package com.elevenst.realtimechat.domain.search.service;

import com.elevenst.realtimechat.domain.search.dto.PopularKeywordResponse;
import com.elevenst.realtimechat.domain.search.entity.SearchHistory;
import com.elevenst.realtimechat.domain.search.repository.SearchHistoryRepository;
import com.elevenst.realtimechat.global.config.CacheConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchKeywordService implements SearchKeywordRecorder {

    private static final int DEFAULT_POPULAR_KEYWORD_LIMIT = 10;
    private static final long POPULAR_KEYWORD_WINDOW_HOURS = 1L;

    private final SearchHistoryRepository searchHistoryRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SearchKeywordRecordCommand command) {
        String normalizedKeyword = normalizeKeyword(command.keyword());
        if (normalizedKeyword == null) {
            return;
        }
        searchHistoryRepository.save(SearchHistory.create(
                command.memberId(),
                command.categoryId(),
                command.guestUuid(),
                normalizedKeyword
        ));
    }

    @Cacheable(
            cacheNames = CacheConfig.POPULAR_KEYWORDS_CACHE,
            key = "T(com.elevenst.realtimechat.global.config.CacheConfig).POPULAR_KEYWORDS_KEY"
    )
    @Transactional(readOnly = true)
    public List<PopularKeywordResponse> getPopularKeywords() {
        LocalDateTime from = LocalDateTime.now().minusHours(POPULAR_KEYWORD_WINDOW_HOURS);
        return searchHistoryRepository.findPopularKeywords(
                        from,
                        PageRequest.of(0, DEFAULT_POPULAR_KEYWORD_LIMIT)
                )
                .stream()
                .map(PopularKeywordResponse::from)
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }
        String trimmed = keyword.trim().toLowerCase(Locale.ROOT);
        if (trimmed.length() > 255) {
            return trimmed.substring(0, 255);
        }
        return trimmed;
    }
}
