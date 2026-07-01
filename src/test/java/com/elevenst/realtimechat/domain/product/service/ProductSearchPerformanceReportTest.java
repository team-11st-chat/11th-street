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
import org.springframework.data.redis.connection.RedisConnectionFactory;
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

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

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

            StatsMeasurement m1Baseline = measureWithStats(() -> searchBaseline(scenario));
            StatsMeasurement m1LocalCache = measureWithStats(() -> searchWithLocalCache(scenario));
            StatsMeasurement m1RemoteCache = measureWithStats(() -> searchWithRemoteCache(scenario));
            StatsMeasurement m2RemoteCache = measureWithStats(() -> searchWithRemoteCache(scenario));
            StatsMeasurement m2LocalCache = measureWithStats(() -> searchWithLocalCache(scenario));
            StatsMeasurement m2Baseline = measureWithStats(() -> searchBaseline(scenario));

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

    private long getLocalHitCount() {
        Cache springCache = cacheManager.getCache(PRODUCT_SEARCH_CACHE);
        if (springCache instanceof org.springframework.cache.caffeine.CaffeineCache caffeineCache) {
            return caffeineCache.getNativeCache().stats().hitCount();
        }
        return 0L;
    }

    private long getLocalMissCount() {
        Cache springCache = cacheManager.getCache(PRODUCT_SEARCH_CACHE);
        if (springCache instanceof org.springframework.cache.caffeine.CaffeineCache caffeineCache) {
            return caffeineCache.getNativeCache().stats().missCount();
        }
        return 0L;
    }

    private long getRemoteHitCount() {
        try (var connection = redisConnectionFactory.getConnection()) {
            java.util.Properties info = connection.info("stats");
            if (info != null) {
                return Long.parseLong(info.getProperty("keyspace_hits", "0"));
            }
        } catch (Exception e) {
            // 무시
        }
        return 0L;
    }

    private long getRemoteMissCount() {
        try (var connection = redisConnectionFactory.getConnection()) {
            java.util.Properties info = connection.info("stats");
            if (info != null) {
                return Long.parseLong(info.getProperty("keyspace_misses", "0"));
            }
        } catch (Exception e) {
            // 무시
        }
        return 0L;
    }

    private long getLocalCacheSize() {
        Cache springCache = cacheManager.getCache(PRODUCT_SEARCH_CACHE);
        if (springCache instanceof org.springframework.cache.caffeine.CaffeineCache caffeineCache) {
            return caffeineCache.getNativeCache().estimatedSize();
        }
        return 0L;
    }

    private String getRedisMemoryUsage() {
        try (var connection = redisConnectionFactory.getConnection()) {
            java.util.Properties info = connection.info("memory");
            if (info != null) {
                return info.getProperty("used_memory_human", "N/A");
            }
        } catch (Exception e) {
            // 무시
        }
        return "N/A";
    }

    private StatsMeasurement measureWithStats(Runnable search) {
        long startLocalHits = getLocalHitCount();
        long startLocalMisses = getLocalMissCount();
        long startRemoteHits = getRemoteHitCount();
        long startRemoteMisses = getRemoteMissCount();

        Measurement measurement = measure(search);

        long endLocalHits = getLocalHitCount();
        long endLocalMisses = getLocalMissCount();
        long endRemoteHits = getRemoteHitCount();
        long endRemoteMisses = getRemoteMissCount();

        return new StatsMeasurement(
                measurement,
                endLocalHits - startLocalHits,
                endLocalMisses - startLocalMisses,
                endRemoteHits - startRemoteHits,
                endRemoteMisses - startRemoteMisses,
                getLocalCacheSize(),
                getRedisMemoryUsage()
        );
    }

    private void printReport(
            SearchMeasurementScenario scenario,
            StatsMeasurement m1Baseline,
            StatsMeasurement m1LocalCache,
            StatsMeasurement m1RemoteCache,
            StatsMeasurement m2Baseline,
            StatsMeasurement m2LocalCache,
            StatsMeasurement m2RemoteCache
    ) {
        double avgBaselineMillis = (m1Baseline.measurement().averageMillis() + m2Baseline.measurement().averageMillis()) / 2.0;
        double avgLocalCacheMillis = (m1LocalCache.measurement().averageMillis() + m2LocalCache.measurement().averageMillis()) / 2.0;
        double avgRemoteCacheMillis = (m1RemoteCache.measurement().averageMillis() + m2RemoteCache.measurement().averageMillis()) / 2.0;
        double avgBaselineThroughput = (m1Baseline.measurement().throughput() + m2Baseline.measurement().throughput()) / 2.0;
        double avgLocalCacheThroughput = (m1LocalCache.measurement().throughput() + m2LocalCache.measurement().throughput()) / 2.0;
        double avgRemoteCacheThroughput = (m1RemoteCache.measurement().throughput() + m2RemoteCache.measurement().throughput()) / 2.0;

        long totalLocalHits = m1LocalCache.localHits() + m2LocalCache.localHits();
        long totalLocalMisses = m1LocalCache.localMisses() + m2LocalCache.localMisses();
        double localHitRate = (totalLocalHits + totalLocalMisses == 0) ? 0.0 : (double) totalLocalHits / (totalLocalHits + totalLocalMisses) * 100.0;

        long totalRemoteHits = m1RemoteCache.remoteHits() + m2RemoteCache.remoteHits();
        long totalRemoteMisses = m1RemoteCache.remoteMisses() + m2RemoteCache.remoteMisses();
        double remoteHitRate = (totalRemoteHits + totalRemoteMisses == 0) ? 0.0 : (double) totalRemoteHits / (totalRemoteHits + totalRemoteMisses) * 100.0;

        System.out.printf(
                "%n[Product search performance: %s]%n"
                        + "dataCount=%d, keyword=%s, categoryId=%s, page=%d, size=%d, iterations=%d, warmUp=%d%n"
                        + "sort=saleStatus(SOLD_OUT last), id desc%n"
                        + "baseline.avgMillis=%.3f, baseline.throughput=%.2f req/s%n"
                        + "localCache.avgMillis=%.3f, localCache.throughput=%.2f req/s, localHits=%d, localMisses=%d, localHitRate=%.2f%%, localCacheSize=%d%n"
                        + "remoteCache.avgMillis=%.3f, remoteCache.throughput=%.2f req/s, remoteHits=%d, remoteMisses=%d, remoteHitRate=%.2f%%, redisMemory=%s%n"
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
                totalLocalHits,
                totalLocalMisses,
                localHitRate,
                m2LocalCache.localCacheSize(),
                avgRemoteCacheMillis,
                avgRemoteCacheThroughput,
                totalRemoteHits,
                totalRemoteMisses,
                remoteHitRate,
                m2RemoteCache.redisMemory(),
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

    private record StatsMeasurement(
            Measurement measurement,
            long localHits,
            long localMisses,
            long remoteHits,
            long remoteMisses,
            long localCacheSize,
            String redisMemory
    ) {
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
