package com.elevenst.realtimechat.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.search.repository.SearchHistoryRepository;
import com.elevenst.realtimechat.domain.search.repository.SearchHistoryRepository.PopularKeywordRow;
import com.elevenst.realtimechat.global.config.CacheConfig;
import com.github.benmanes.caffeine.cache.Policy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {CacheConfig.class, SearchKeywordService.class})
class SearchKeywordServiceCacheTest {

    @Autowired
    private SearchKeywordService searchKeywordService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private SearchHistoryRepository searchHistoryRepository;

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache(CacheConfig.POPULAR_KEYWORDS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void popularKeywordsCache_usesFixedKeyAndOneMinuteTtl() {
        Cache cache = cacheManager.getCache(CacheConfig.POPULAR_KEYWORDS_CACHE);

        assertThat(cache).isNotNull();
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = nativeCaffeineCache(cache);
        Policy.FixedExpiration<Object, Object> expiration = nativeCache.policy().expireAfterWrite().orElseThrow();

        assertThat(expiration.getExpiresAfter(TimeUnit.SECONDS))
                .isEqualTo(CacheConfig.POPULAR_KEYWORDS_TTL.toSeconds());
    }

    @Test
    void getPopularKeywords_reusesCachedResponseWithPopularKeywordsKey() {
        PopularKeywordRow row = row("keyboard", 2L);
        when(searchHistoryRepository.findPopularKeywords(any(), any(Pageable.class)))
                .thenReturn(List.of(row));

        searchKeywordService.getPopularKeywords();
        searchKeywordService.getPopularKeywords();

        verify(searchHistoryRepository, times(1)).findPopularKeywords(any(), any(Pageable.class));

        Cache cache = cacheManager.getCache(CacheConfig.POPULAR_KEYWORDS_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.get(CacheConfig.POPULAR_KEYWORDS_KEY)).isNotNull();
    }

    private PopularKeywordRow row(String keyword, long searchCount) {
        PopularKeywordRow row = mock(PopularKeywordRow.class);
        when(row.getKeyword()).thenReturn(keyword);
        when(row.getSearchCount()).thenReturn(searchCount);
        return row;
    }

    @SuppressWarnings("unchecked")
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCaffeineCache(Cache cache) {
        return (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();
    }
}
