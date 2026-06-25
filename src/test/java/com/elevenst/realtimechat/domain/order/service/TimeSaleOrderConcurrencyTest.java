package com.elevenst.realtimechat.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

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
import com.elevenst.realtimechat.global.support.LockManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class TimeSaleOrderConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(TimeSaleOrderConcurrencyTest.class);

    @MockitoBean
    private RedissonClient redissonClient;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private LockManager lockManager;

    @Autowired
    private TimeSaleOrderFacade timeSaleOrderFacade;

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

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private Long timeSaleId;

    @BeforeEach
    void setUp() {
        cleanUp();
        configureLockManager();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("타임세일 Lock 적용 후 지정 수량보다 많은 주문이 생성되지 않는다")
    void timeSaleOverSellingIsPrevented() throws InterruptedException {
        int limitedStock = 5;
        int threadCount = 30;
        prepareTimeSale(limitedStock);
        List<Long> memberIds = createMembers(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        runConcurrently(threadCount, threadNumber -> {
            Long memberId = memberIds.get(threadNumber - 1);
            timeSaleOrderFacade.orderTimeSale(memberId, timeSaleId, UUID.randomUUID().toString(),
                    new TimeSaleOrderRequest(1));
            successCount.incrementAndGet();
        }, rejectedCount);

        int remainingStock = timeSaleStockRepository.findByTimeSaleId(timeSaleId)
                .orElseThrow()
                .getRemainingQuantity();
        log.info("[time-sale stock] limited={}, success={}, rejected={}, remaining={}",
                limitedStock, successCount.get(), rejectedCount.get(), remainingStock);

        assertThat(successCount.get()).isEqualTo(limitedStock);
        assertThat(timeSaleOrderRepository.count()).isEqualTo(limitedStock);
        assertThat(remainingStock).isZero();
    }

    @Test
    @DisplayName("타임세일 Lock 적용 후 동일 고객의 동시 주문은 1건만 성공한다")
    void duplicateOrderBySameMemberIsPrevented() throws InterruptedException {
        int threadCount = 10;
        prepareTimeSale(threadCount);
        Long memberId = createMembers(1).get(0);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        runConcurrently(threadCount, threadNumber -> {
            timeSaleOrderFacade.orderTimeSale(memberId, timeSaleId, UUID.randomUUID().toString(),
                    new TimeSaleOrderRequest(1));
            successCount.incrementAndGet();
        }, rejectedCount);

        log.info("[duplicate order] requests={}, success={}, rejected={}",
                threadCount, successCount.get(), rejectedCount.get());

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(timeSaleOrderRepository.count()).isEqualTo(1);
    }

    private void configureLockManager() {
        locks.clear();
        when(lockManager.tryLock(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return locks.computeIfAbsent(key, ignored -> new ReentrantLock()).tryLock(3, TimeUnit.SECONDS);
        });
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            ReentrantLock lock = locks.get(key);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            return null;
        }).when(lockManager).unlock(anyString());
    }

    private void prepareTimeSale(int stock) {
        Category root = categoryRepository.save(Category.createRoot("Digital", 1));
        Category category = categoryRepository.save(Category.createChild(root, "Audio", 1));
        Product product = productRepository.save(
                Product.create(1L, category, "Time sale test product", new BigDecimal("10000"), 100_000));

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
                    Member.create("buyer" + i + "@example.com", "password123!", "buyer" + i));
            ids.add(member.getId());
        }
        return ids;
    }

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
                    log.warn("[thread {}] unexpected exception: {}", threadNumber, e.toString());
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
