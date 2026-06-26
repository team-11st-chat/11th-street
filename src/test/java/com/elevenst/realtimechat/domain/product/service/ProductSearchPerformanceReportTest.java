package com.elevenst.realtimechat.domain.product.service;

import static com.elevenst.realtimechat.global.config.CacheConfig.PRODUCT_SEARCH_CACHE;
import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.domain.product.dto.ProductPageResponse;
import com.elevenst.realtimechat.domain.product.service.ProductSearchDummyDataSeeder.SeedResult;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "PERFORMANCE_TEST", matches = "true")
@Tag("integration")
class ProductSearchPerformanceReportTest {

    private static final int ITERATIONS = 100;
    private static final int WARMUP_ITERATIONS = 100;
    private static final int FIRST_PAGE = 0;
    private static final int SECOND_PAGE = 1;
    private static final int PAGE_SIZE = 20;

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private ProductSearchDummyDataSeeder dummyDataSeeder;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    @Qualifier("redisCacheManager")
    private CacheManager redisCacheManager;

    private List<SearchMeasurementScenario> scenarios;

    @BeforeEach
    void setUp() {
        clearProductSearchCache();
        SeedResult seedResult = dummyDataSeeder.resetAndSeedDefaultProducts();
        Long categoryId = seedResult.leafCategoryIds().get(0);

        scenarios = List.of(
                new SearchMeasurementScenario(
                        "keyword_only_first_page",
                        ProductSearchDummyDataSeeder.PRIMARY_KEYWORD,
                        null,
                        FIRST_PAGE,
                        PAGE_SIZE,
                        seedResult.productCount()
                ),
                new SearchMeasurementScenario(
                        "keyword_and_category_first_page",
                        ProductSearchDummyDataSeeder.PRIMARY_KEYWORD,
                        categoryId,
                        FIRST_PAGE,
                        PAGE_SIZE,
                        seedResult.productCount()
                ),
                new SearchMeasurementScenario(
                        "broad_keyword_second_page",
                        ProductSearchDummyDataSeeder.SECONDARY_KEYWORD,
                        null,
                        SECOND_PAGE,
                        PAGE_SIZE,
                        seedResult.productCount()
                )
        );
    }

    @Test
    void compareBaselineAndLocalCacheSearchPerformance() {
        for (SearchMeasurementScenario scenario : scenarios) {
            assertSameSearchResult(scenario);

            warmUp(() -> searchBaseline(scenario));
            warmUp(() -> searchWithLocalCache(scenario));
            warmUp(() -> searchWithRemoteCache(scenario));

            Measurement m1Baseline = measure(() -> searchBaseline(scenario));
            Measurement m1LocalCache = measure(() -> searchWithLocalCache(scenario));
            Measurement m1RemoteCache = measure(() -> searchWithRemoteCache(scenario));
            Measurement m2RemoteCache = measure(() -> searchWithRemoteCache(scenario));
            Measurement m2LocalCache = measure(() -> searchWithLocalCache(scenario));
            Measurement m2Baseline = measure(() -> searchBaseline(scenario));

            printReport(scenario, m1Baseline, m1LocalCache, m1RemoteCache, m2Baseline, m2LocalCache, m2RemoteCache);
        }
    }

    private void assertSameSearchResult(SearchMeasurementScenario scenario) {
        ProductPageResponse baseline = searchBaseline(scenario);
        ProductPageResponse localCache = searchWithLocalCache(scenario);
        ProductPageResponse remoteCache = searchWithRemoteCache(scenario);

        assertThat(localCache).isEqualTo(baseline);
        assertThat(remoteCache).isEqualTo(baseline);
    }

    private ProductPageResponse searchBaseline(SearchMeasurementScenario scenario) {
        return productSearchService.searchProducts(
                scenario.keyword(),
                scenario.categoryId(),
                scenario.page(),
                scenario.size()
        );
    }

    private ProductPageResponse searchWithLocalCache(SearchMeasurementScenario scenario) {
        return productSearchService.searchProductsWithCache(
                scenario.keyword(),
                scenario.categoryId(),
                scenario.page(),
                scenario.size()
        );
    }

    private ProductPageResponse searchWithRemoteCache(SearchMeasurementScenario scenario) {
        return productSearchService.searchProductsWithRemoteCache(
                scenario.keyword(),
                scenario.categoryId(),
                scenario.page(),
                scenario.size()
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

    private void printReport(
            SearchMeasurementScenario scenario,
            Measurement m1Baseline,
            Measurement m1LocalCache,
            Measurement m1RemoteCache,
            Measurement m2Baseline,
            Measurement m2LocalCache,
            Measurement m2RemoteCache
    ) {
        double avgBaselineMillis = (m1Baseline.averageMillis() + m2Baseline.averageMillis()) / 2.0;
        double avgLocalCacheMillis = (m1LocalCache.averageMillis() + m2LocalCache.averageMillis()) / 2.0;
        double avgRemoteCacheMillis = (m1RemoteCache.averageMillis() + m2RemoteCache.averageMillis()) / 2.0;
        double avgBaselineThroughput = (m1Baseline.throughput() + m2Baseline.throughput()) / 2.0;
        double avgLocalCacheThroughput = (m1LocalCache.throughput() + m2LocalCache.throughput()) / 2.0;
        double avgRemoteCacheThroughput = (m1RemoteCache.throughput() + m2RemoteCache.throughput()) / 2.0;

        System.out.printf(
                "%n[Product search performance: %s]%n"
                        + "dataCount=%d, keyword=%s, categoryId=%s, page=%d, size=%d, iterations=%d, warmUp=%d%n"
                        + "sort=saleStatus(SOLD_OUT last), id desc%n"
                        + "baseline.avgMillis=%.3f, baseline.throughput=%.2f req/s%n"
                        + "localCache.avgMillis=%.3f, localCache.throughput=%.2f req/s%n"
                        + "remoteCache.avgMillis=%.3f, remoteCache.throughput=%.2f req/s%n"
                        + "localImprovement.avgResponseTime=%.2fx, localImprovement.throughput=%.2fx%n"
                        + "remoteImprovement.avgResponseTime=%.2fx, remoteImprovement.throughput=%.2fx%n",
                scenario.name(),
                scenario.productCount(),
                scenario.keyword(),
                scenario.categoryId(),
                scenario.page(),
                scenario.size(),
                ITERATIONS,
                WARMUP_ITERATIONS,
                avgBaselineMillis,
                avgBaselineThroughput,
                avgLocalCacheMillis,
                avgLocalCacheThroughput,
                avgRemoteCacheMillis,
                avgRemoteCacheThroughput,
                avgBaselineMillis / avgLocalCacheMillis,
                avgLocalCacheThroughput / avgBaselineThroughput,
                avgBaselineMillis / avgRemoteCacheMillis,
                avgRemoteCacheThroughput / avgBaselineThroughput
        );
    }

    private void clearProductSearchCache() {
        clearProductSearchCache(cacheManager);
        clearProductSearchCache(redisCacheManager);
    }

    private void clearProductSearchCache(CacheManager manager) {
        Cache cache = manager.getCache(PRODUCT_SEARCH_CACHE);
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

    private record SearchMeasurementScenario(
            String name,
            String keyword,
            Long categoryId,
            int page,
            int size,
            int productCount
    ) {
    }
}
