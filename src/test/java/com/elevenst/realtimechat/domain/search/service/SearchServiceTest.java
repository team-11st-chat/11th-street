package com.elevenst.realtimechat.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.elevenst.realtimechat.domain.search.entity.SearchHistory;
import com.elevenst.realtimechat.domain.search.repository.SearchHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    void record_savesNormalizedKeyword_whenKeywordIsValid() {
        // given
        SearchKeywordRecordCommand command = SearchKeywordRecordCommand.guest("  MacBook Pro  ", "guest_123", 11L);

        // when
        searchService.record(command);

        // then
        ArgumentCaptor<SearchHistory> captor = ArgumentCaptor.forClass(SearchHistory.class);
        verify(searchHistoryRepository).save(captor.capture());
        
        SearchHistory saved = captor.getValue();
        assertThat(saved.getKeyword()).isEqualTo("macbook pro");
        assertThat(saved.getGuestUuid()).isEqualTo("guest_123");
        assertThat(saved.getCategoryId()).isEqualTo(11L);
    }

    @Test
    void record_truncatesKeyword_whenKeywordExceeds255Chars() {
        // given
        String longKeyword = "a".repeat(300);
        SearchKeywordRecordCommand command = SearchKeywordRecordCommand.guest(longKeyword, "guest_123", 11L);

        // when
        searchService.record(command);

        // then
        ArgumentCaptor<SearchHistory> captor = ArgumentCaptor.forClass(SearchHistory.class);
        verify(searchHistoryRepository).save(captor.capture());

        SearchHistory saved = captor.getValue();
        assertThat(saved.getKeyword()).hasSize(255);
        assertThat(saved.getKeyword()).isEqualTo("a".repeat(255));
    }

    @Test
    void record_doesNotSave_whenKeywordIsBlank() {
        // given
        SearchKeywordRecordCommand command = SearchKeywordRecordCommand.guest("   ", "guest_123", 11L);

        // when
        searchService.record(command);

        // then
        verify(searchHistoryRepository, never()).save(any());
    }

    @Test
    void record_doesNotSave_whenKeywordIsNull() {
        // given
        SearchKeywordRecordCommand command = SearchKeywordRecordCommand.guest(null, "guest_123", 11L);

        // when
        searchService.record(command);

        // then
        verify(searchHistoryRepository, never()).save(any());
    }
}
