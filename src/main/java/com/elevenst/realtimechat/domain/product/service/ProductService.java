package com.elevenst.realtimechat.domain.product.service;

import com.elevenst.realtimechat.domain.product.dto.ProductCreateRequest;
import com.elevenst.realtimechat.domain.product.dto.ProductPageResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductUpdateRequest;
import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.exception.ProductErrorCode;
import com.elevenst.realtimechat.domain.product.exception.ProductException;
import com.elevenst.realtimechat.domain.product.repository.CategoryRepository;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecordCommand;
import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecorder;
import com.elevenst.realtimechat.global.config.CacheConfig;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SearchKeywordRecorder searchKeywordRecorder;
    private final ProductSearchService productSearchService;

    @CacheEvict(cacheNames = CacheConfig.PRODUCT_SEARCH_CACHE, allEntries = true)
    @Transactional
    public ProductResponse createProduct(Long sellerId, ProductCreateRequest request) {
        Category category = getCategory(request.categoryId());
        Product product = Product.create(sellerId, category, request.name(), request.price(), request.stockQuantity());
        return ProductResponse.from(productRepository.save(product));
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCT_SEARCH_CACHE, allEntries = true)
    @Transactional
    public ProductResponse updateProduct(Long sellerId, Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
        Category category = request.categoryId() == null ? null : getCategory(request.categoryId());

        product.update(
                sellerId,
                category,
                request.name(),
                request.price(),
                request.stockQuantity(),
                request.saleStatus()
        );
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        return ProductResponse.from(getProductEntity(productId));
    }

    @Transactional
    public ProductPageResponse searchProducts(String keyword, Long categoryId, int page, int size, String guestId) {
        return searchProducts(keyword, categoryId, page, size, guestId, false);
    }

    @Transactional
    public ProductPageResponse searchProductsV2(String keyword, Long categoryId, int page, int size, String guestId) {
        return searchProducts(keyword, categoryId, page, size, guestId, true);
    }

    private ProductPageResponse searchProducts(String keyword, Long categoryId, int page, int size, String guestId, boolean cacheable) {
        validatePageRequest(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null) {
            recordSearchKeyword(keyword, categoryId, guestId);
        }

        if (cacheable) {
            return productSearchService.searchProductsWithCache(normalizedKeyword, categoryId, page, size);
        }
        return productSearchService.searchProducts(normalizedKeyword, categoryId, page, size);
    }

    private Product getProductEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ProductException(ProductErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private void recordSearchKeyword(String keyword, Long categoryId, String guestId) {
        try {
            searchKeywordRecorder.record(SearchKeywordRecordCommand.guest(keyword, guestId, categoryId));
        } catch (Exception e) {
            log.error("Failed to record search keyword: keyword={}, guestId={}, categoryId={}", keyword, guestId, categoryId, e);
        }
    }
}
