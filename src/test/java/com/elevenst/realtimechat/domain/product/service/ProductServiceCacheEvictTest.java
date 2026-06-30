package com.elevenst.realtimechat.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.product.dto.ProductUpdateRequest;
import com.elevenst.realtimechat.domain.category.entity.Category;
import com.elevenst.realtimechat.domain.category.service.CategoryQueryService;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import com.elevenst.realtimechat.domain.search.service.SearchKeywordRecorder;
import com.elevenst.realtimechat.global.config.CacheConfig;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {CacheConfig.class, ProductService.class, ProductSearchCacheEvictor.class})
@Tag("integration")
class ProductServiceCacheEvictTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CategoryQueryService categoryQueryService;

    @MockitoBean
    private SearchKeywordRecorder searchKeywordRecorder;

    @MockitoBean
    private ProductSearchService productSearchService;

    @BeforeEach
    void setUp() {
        Cache cache = productSearchCache();
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void updateProduct_evictsAllProductSearchCacheEntries() {
        Category category = Category.createChild(Category.createRoot("Digital", 1), "Audio", 1);
        Product product = Product.create(1L, category, "Galaxy Buds", new BigDecimal("199000"), 10);
        String cacheKey = "product_search:galaxy:null:0:20";
        Cache cache = productSearchCache();

        cache.put(cacheKey, "cached-response");
        when(productRepository.findById(1001L)).thenReturn(Optional.of(product));

        productService.updateProduct(
                1L,
                1001L,
                new ProductUpdateRequest("Galaxy Buds Pro", null, null, null, SaleStatus.ON_SALE)
        );

        assertThat(cache.get(cacheKey)).isNull();
    }

    private Cache productSearchCache() {
        Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_SEARCH_CACHE);
        assertThat(cache).isNotNull();
        return cache;
    }
}
