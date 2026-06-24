package com.elevenst.realtimechat.domain.search.support;

import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecorder;
import org.springframework.stereotype.Component;

@Component
public class NoOpSearchKeywordRecorder implements SearchKeywordRecorder {

    @Override
    public void record(String keyword, String guestId) {
        // TODO: 검색어 기록 구현 전까지 no-op 처리
    }
}
