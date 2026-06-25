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
import org.junit.jupiter.api.Tag;
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
@Tag("integration")
class ProductSearchPerformanceReportTest {

    private static final int PRODUCT_COUNT = 50_000;
    private static final int ITERATIONS = 100;
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

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final int WARMUP_ITERATIONS = 100;

    @BeforeEach
    void setUp() {
        clearProductSearchCache();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category root = categoryRepository.save(Category.createRoot("Performance", 1));
        Category leaf = categoryRepository.save(Category.createChild(root, "Search", 1));

        String sql = "INSERT INTO product (seller_id, category_id, name, price, stock_quantity, sale_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                ps.setLong(1, 1L);
                ps.setLong(2, leaf.getId());
                ps.setString(3, "Cache-Target Product " + i);
                ps.setBigDecimal(4, BigDecimal.valueOf(10_000L + i));
                ps.setInt(5, i % 10 == 0 ? 0 : 100);
                ps.setString(6, i % 10 == 0 ? "SOLD_OUT" : "ON_SALE");
                ps.setTimestamp(7, java.sql.Timestamp.valueOf(now));
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(now));
            }

            @Override
            public int getBatchSize() {
                return PRODUCT_COUNT;
            }
        });
    }

    @Test
    void compareBaselineAndLocalCacheSearchPerformance() {
        ProductPageResponse baseline = productSearchService.searchProducts(KEYWORD, null, PAGE, SIZE);
        ProductPageResponse localCache = productSearchService.searchProductsWithCache(KEYWORD, null, PAGE, SIZE);

        assertThat(localCache).isEqualTo(baseline);

        // 1. Warm-up (JVM JIT 컴파일 및 캐시 준비 상태 보장)
        warmUp(() -> productSearchService.searchProducts(KEYWORD, null, PAGE, SIZE));
        warmUp(() -> productSearchService.searchProductsWithCache(KEYWORD, null, PAGE, SIZE));

        // 2. Measure with order swapping to avoid bias
        // Session 1: Baseline -> Cache
        Measurement m1Baseline = measure(
                () -> productSearchService.searchProducts(KEYWORD, null, PAGE, SIZE)
        );
        Measurement m1Cache = measure(
                () -> productSearchService.searchProductsWithCache(KEYWORD, null, PAGE, SIZE)
        );

        // Session 2: Cache -> Baseline
        Measurement m2Cache = measure(
                () -> productSearchService.searchProductsWithCache(KEYWORD, null, PAGE, SIZE)
        );
        Measurement m2Baseline = measure(
                () -> productSearchService.searchProducts(KEYWORD, null, PAGE, SIZE)
        );

        // Average metrics
        double avgBaselineMillis = (m1Baseline.averageMillis() + m2Baseline.averageMillis()) / 2.0;
        double avgCacheMillis = (m1Cache.averageMillis() + m2Cache.averageMillis()) / 2.0;

        double avgBaselineThroughput = (m1Baseline.throughput() + m2Baseline.throughput()) / 2.0;
        double avgCacheThroughput = (m1Cache.throughput() + m2Cache.throughput()) / 2.0;

        System.out.printf(
                "%n[Product search performance]%n" +
                        "dataCount=%d, keyword=%s, page=%d, size=%d, iterations=%d (warmUp=%d)%n" +
                        "baseline.avgMillis=%.3f, baseline.throughput=%.2f req/s%n" +
                        "localCache.avgMillis=%.3f, localCache.throughput=%.2f req/s%n" +
                        "improvement.avgResponseTime=%.2fx, improvement.throughput=%.2fx%n",
                PRODUCT_COUNT,
                KEYWORD,
                PAGE,
                SIZE,
                ITERATIONS,
                WARMUP_ITERATIONS,
                avgBaselineMillis,
                avgBaselineThroughput,
                avgCacheMillis,
                avgCacheThroughput,
                avgBaselineMillis / avgCacheMillis,
                avgCacheThroughput / avgBaselineThroughput
        );
    }

    private void warmUp(Runnable search) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            search.run();
        }
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
