package com.elevenst.realtimechat.domain.product.service;

import static com.elevenst.realtimechat.global.config.CacheConfig.PRODUCT_SEARCH_CACHE;
import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.domain.product.dto.ProductPageResponse;
import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.repository.CategoryRepository;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "PERFORMANCE_TEST", matches = "true")
class ProductSearchPerformanceReportTest {

    private static final int PRODUCT_COUNT = 50_000;
    private static final int ITERATIONS = 300;
    private static final int PAGE = 0;
    private static final int SIZE = 20;
    private static final String KEYWORD = "cache-target";

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        clearProductSearchCache();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category root = categoryRepository.save(Category.createRoot("Performance", 1));
        Category leaf = categoryRepository.save(Category.createChild(root, "Search", 1));

        List<Product> products = new ArrayList<>(PRODUCT_COUNT);
        for (int i = 0; i < PRODUCT_COUNT; i++) {
            products.add(Product.create(
                    1L,
                    leaf,
                    "Cache-Target Product " + i,
                    BigDecimal.valueOf(10_000L + i),
                    i % 10 == 0 ? 0 : 100
            ));
        }
        productRepository.saveAll(products);
        productRepository.flush();
    }

    @Test
    void compareBaselineAndLocalCacheSearchPerformance() {
        ProductPageResponse baseline = productSearchService.searchProducts(KEYWORD, null, PAGE, SIZE);
        ProductPageResponse localCache = productSearchService.searchProductsWithCache(KEYWORD, null, PAGE, SIZE);

        assertThat(localCache).isEqualTo(baseline);

        Measurement baselineMeasurement = measure(
                () -> productSearchService.searchProducts(KEYWORD, null, PAGE, SIZE)
        );

        Measurement localCacheMeasurement = measure(
                () -> productSearchService.searchProductsWithCache(KEYWORD, null, PAGE, SIZE)
        );

        System.out.printf(
                "%n[Product search performance]%n" +
                        "dataCount=%d, keyword=%s, page=%d, size=%d, iterations=%d%n" +
                        "baseline.totalMillis=%.3f, baseline.avgMillis=%.3f, baseline.throughput=%.2f req/s%n" +
                        "localCache.totalMillis=%.3f, localCache.avgMillis=%.3f, localCache.throughput=%.2f req/s%n" +
                        "improvement.avgResponseTime=%.2fx, improvement.throughput=%.2fx%n",
                PRODUCT_COUNT,
                KEYWORD,
                PAGE,
                SIZE,
                ITERATIONS,
                baselineMeasurement.totalMillis(),
                baselineMeasurement.averageMillis(),
                baselineMeasurement.throughput(),
                localCacheMeasurement.totalMillis(),
                localCacheMeasurement.averageMillis(),
                localCacheMeasurement.throughput(),
                baselineMeasurement.averageMillis() / localCacheMeasurement.averageMillis(),
                localCacheMeasurement.throughput() / baselineMeasurement.throughput()
        );
    }

    private Measurement measure(Runnable search) {
        long startedAt = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            search.run();
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        return new Measurement(elapsed);
    }

    private void clearProductSearchCache() {
        Cache cache = cacheManager.getCache(PRODUCT_SEARCH_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private record Measurement(Duration elapsed) {

        double totalMillis() {
            return elapsed.toNanos() / 1_000_000.0;
        }

        double averageMillis() {
            return totalMillis() / ITERATIONS;
        }

        double throughput() {
            return ITERATIONS / (elapsed.toNanos() / 1_000_000_000.0);
        }
    }
}
