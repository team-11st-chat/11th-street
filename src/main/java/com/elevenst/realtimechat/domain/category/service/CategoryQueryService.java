package com.elevenst.realtimechat.domain.category.service;

import com.elevenst.realtimechat.domain.category.entity.Category;
import com.elevenst.realtimechat.domain.category.exception.CategoryErrorCode;
import com.elevenst.realtimechat.domain.category.exception.CategoryException;
import com.elevenst.realtimechat.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryService {

    private final CategoryRepository categoryRepository;

    public Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }
}
