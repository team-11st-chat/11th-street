package com.elevenst.realtimechat.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderRequest;
import com.elevenst.realtimechat.domain.order.repository.TimeSaleOrderRepository;
import com.elevenst.realtimechat.domain.product.entity.Category;
import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.repository.CategoryRepository;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSale;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStock;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleRepository;
import com.elevenst.realtimechat.domain.promotion.repository.TimeSaleStockRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Issue #26 - 타임세일 Lock 미적용 동시성 실패(재현) 테스트.
 *
 * <p>현재 운영 코드의 분산 락은 {@link com.elevenst.realtimechat.global.support.FakeLockManager}(no-op)
 * 로 대체되어 있어 실제 직렬화가 일어나지 않는다. 이 상태에서 동시 주문을 발생시켜
 * <b>타임세일 초과 판매</b>와 <b>동일 고객 중복 주문</b>이 재현됨을 드러낸다.
 *
 * <p>각 테스트는 "올바른 불변식"을 단언하므로 현재(락 미적용)에는 실패(RED)한다.
 * 다른 Issue 에서 실제 Redisson 분산 락이 {@code FakeLockManager} 를 대체하면 통과(GREEN)로 전환되어야 한다.
 *
 * <p>반복 실행 조건(모든 동시성 테스트 공통):
 * <ul>
 *   <li>{@code readyLatch} 로 모든 스레드가 준비될 때까지 대기 후 {@code startLatch} 로 동시 출발시켜 경합을 강제한다.</li>
 *   <li>스레드 수는 한정 수량보다 크게 잡아 경합을 보장한다.</li>
 *   <li>매 실행마다 {@code @BeforeEach}/{@code @AfterEach} 에서 데이터를 초기화해 반복 실행이 가능하다.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class TimeSaleOrderConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(TimeSaleOrderConcurrencyTest.class);

    @MockitoBean
    private RedissonClient redissonClient;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TimeSaleOrderService timeSaleOrderService;

    @Autowired
    private TimeSaleOrderRepository timeSaleOrderRepository;
    @Autowired
    private TimeSaleRepository timeSaleRepository;
    @Autowired
    private TimeSaleStockRepository timeSaleStockRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private MemberRepository memberRepository;

    private Long timeSaleId;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @Disabled("Issue #26 - 분산 락 구현 후 활성화")
    @DisplayName("[재현] 분산 락 미적용 시 한정 수량보다 많은 주문이 성공하여 타임세일 초과 판매가 발생한다")
    void timeSaleOverSellingReproduced() throws InterruptedException {
        int limitedStock = 5;
        int threadCount = 30;
        prepareTimeSale(limitedStock);

        List<Long> memberIds = createMembers(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        runConcurrently(threadCount, threadNumber -> {
            Long memberId = memberIds.get(threadNumber - 1);
            timeSaleOrderService.orderTimeSale(memberId, timeSaleId, UUID.randomUUID().toString(),
                    new TimeSaleOrderRequest(1));
            successCount.incrementAndGet();
        }, rejectedCount);

        int remainingStock = timeSaleStockRepository.findByTimeSaleId(timeSaleId).orElseThrow().getRemainingQuantity();
        log.info("[초과 판매] 한정 수량={}, 성공={}, 거절={}, 잔여={}",
                limitedStock, successCount.get(), rejectedCount.get(), remainingStock);

        // 락이 적용되면 정확히 한정 수량만큼만 성공해야 한다. (현재는 초과 판매로 RED)
        assertThat(successCount.get()).isEqualTo(limitedStock);
        assertThat(timeSaleOrderRepository.count()).isEqualTo(limitedStock);
    }

    @Test
    @Disabled("Issue #26 - 분산 락 구현 후 활성화")
    @DisplayName("[재현] 분산 락 미적용 시 동일 고객의 동시 주문이 모두 성공하여 중복 주문이 발생한다")
    void duplicateOrderBySameMemberReproduced() throws InterruptedException {
        int threadCount = 10;
        // 동일 고객 1명이 여러 번 동시에 주문해도 1건만 성공해야 한다. 수량은 경합과 무관하게 충분히 둔다.
        prepareTimeSale(threadCount);

        Long memberId = createMembers(1).get(0);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        runConcurrently(threadCount, threadNumber -> {
            timeSaleOrderService.orderTimeSale(memberId, timeSaleId, UUID.randomUUID().toString(),
                    new TimeSaleOrderRequest(1));
            successCount.incrementAndGet();
        }, rejectedCount);

        log.info("[중복 주문] 동일 고객 동시 요청={}, 성공={}, 거절={}", threadCount, successCount.get(), rejectedCount.get());

        // 1인 1주문 불변식: 1건만 성공해야 한다. (현재는 중복 주문으로 RED)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(timeSaleOrderRepository.count()).isEqualTo(1);
    }

    /**
     * 한정 수량 {@code stock} 짜리 ONGOING 타임세일과 재고를 구성하고 {@code timeSaleId} 에 저장한다.
     */
    private void prepareTimeSale(int stock) {
        Category root = categoryRepository.save(Category.createRoot("Digital", 1));
        Category category = categoryRepository.save(Category.createChild(root, "Audio", 1));
        // 상품 재고는 타임세일 한정 수량 차감이 먼저 막히도록 충분히 크게 둔다.
        Product product = productRepository.save(
                Product.create(1L, category, "동시성 테스트 상품", new BigDecimal("10000"), 100_000));

        LocalDateTime now = LocalDateTime.now();
        TimeSale timeSale = timeSaleRepository.save(
                new TimeSale(product, new BigDecimal("9000"), now.minusHours(1), now.plusHours(1)));
        timeSaleStockRepository.save(new TimeSaleStock(timeSale, stock));

        this.timeSaleId = timeSale.getId();
    }

    private List<Long> createMembers(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Member member = memberRepository.save(
                    Member.create("buyer" + i + "@example.com", "password123!", "구매자" + i));
            ids.add(member.getId());
        }
        return ids;
    }

    /**
     * {@code threadCount} 개의 스레드를 동시에 출발시켜 {@code task} 를 실행한다.
     * 비즈니스 예외(중복/품절 등)는 거절로 집계하고, 그 외 예외는 로깅한다.
     */
    private void runConcurrently(int threadCount, ConcurrentTask task, AtomicInteger rejectedCount)
            throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i + 1;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    task.run(threadNumber);
                } catch (com.elevenst.realtimechat.global.exception.BusinessException
                         | org.springframework.dao.DataAccessException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("[스레드-{}] 예상치 못한 예외: {}", threadNumber, e.toString());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        try {
            readyLatch.await();
            startLatch.countDown();

            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        } finally {
            executorService.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ConcurrentTask {
        void run(int threadNumber);
    }

    private void cleanUp() {
        timeSaleOrderRepository.deleteAll();
        timeSaleStockRepository.deleteAll();
        timeSaleRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }
}
