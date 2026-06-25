package com.elevenst.realtimechat.domain.search.controller;

import com.elevenst.realtimechat.domain.search.dto.PopularKeywordResponse;
import com.elevenst.realtimechat.domain.search.service.SearchService;
import com.elevenst.realtimechat.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/popular-keywords")
    public ApiResponse<List<PopularKeywordResponse>> getPopularKeywords() {
        return ApiResponse.success("Popular keywords retrieved successfully.", searchService.getPopularKeywords());
    }
}
