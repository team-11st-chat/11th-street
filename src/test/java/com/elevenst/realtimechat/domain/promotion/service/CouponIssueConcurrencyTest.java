package com.elevenst.realtimechat.domain.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;
import com.elevenst.realtimechat.domain.promotion.entity.CouponPolicy;
import com.elevenst.realtimechat.domain.promotion.entity.DiscountType;
import com.elevenst.realtimechat.domain.promotion.repository.CouponPolicyRepository;
import com.elevenst.realtimechat.domain.promotion.repository.IssuedCouponRepository;
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

/**
 * Issue #26 - 쿠폰 Lock 미적용 동시성 실패(재현) 테스트.
 *
 * <p>현재 운영 코드의 분산 락은 {@link com.elevenst.realtimechat.global.support.FakeLockManager}(no-op)
 * 로 대체되어 있어 실제 직렬화가 일어나지 않는다. 이 상태에서 동시 발급을 발생시켜
 * <b>선착순 쿠폰 초과 발급</b>과 <b>동일 고객 중복 발급</b> 시나리오를 재현한다.
 *
 * <p>각 테스트는 "올바른 불변식"을 단언하므로 락 미적용 상태에서는 초과 발급 케이스가 실패(RED)한다.
 * 다른 Issue 에서 실제 Redisson 분산 락이 {@code FakeLockManager} 를 대체하면 통과(GREEN)로 전환되어야 한다.
 *
 * <p>반복 실행 조건: {@code readyLatch}/{@code startLatch} 배리어로 동시 출발을 강제하고,
 * 매 실행마다 {@code @BeforeEach}/{@code @AfterEach} 에서 데이터를 초기화한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class CouponIssueConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(CouponIssueConcurrencyTest.class);

    @MockitoBean
    private RedissonClient redissonClient;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CouponIssueFacade couponIssueFacade;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;
    @Autowired
    private IssuedCouponRepository issuedCouponRepository;
    @Autowired
    private MemberRepository memberRepository;

    private Long couponPolicyId;

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
    @DisplayName("[재현] 분산 락 미적용 시 선착순 수량보다 많은 발급이 성공하여 쿠폰 초과 발급이 발생한다")
    void couponOverIssuingReproduced() throws InterruptedException {
        int totalQuantity = 5;
        int threadCount = 30;
        prepareCouponPolicy(totalQuantity);

        List<Long> memberIds = createMembers(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        runConcurrently(threadCount, threadNumber -> {
            Long memberId = memberIds.get(threadNumber - 1);
            couponIssueFacade.issueCoupon(memberId, couponPolicyId, UUID.randomUUID().toString());
            successCount.incrementAndGet();
        }, rejectedCount);

        int remaining = couponPolicyRepository.findById(couponPolicyId).orElseThrow().getRemainingQuantity();
        log.info("[초과 발급] 선착순 수량={}, 성공={}, 거절={}, 잔여={}",
                totalQuantity, successCount.get(), rejectedCount.get(), remaining);

        // 락이 적용되면 정확히 선착순 수량만큼만 성공해야 한다. (현재는 초과 발급으로 RED)
        assertThat(successCount.get()).isEqualTo(totalQuantity);
        assertThat(issuedCouponRepository.count()).isEqualTo(totalQuantity);
    }

    @Test
    @DisplayName("[재현] 동일 고객의 동시 발급 요청은 1장만 성공해야 한다(DB 유니크 제약으로 보호)")
    void duplicateIssueBySameMemberReproduced() throws InterruptedException {
        int threadCount = 10;
        prepareCouponPolicy(threadCount);

        Long memberId = createMembers(1).get(0);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        runConcurrently(threadCount, threadNumber -> {
            couponIssueFacade.issueCoupon(memberId, couponPolicyId, UUID.randomUUID().toString());
            successCount.incrementAndGet();
        }, rejectedCount);

        log.info("[중복 발급] 동일 고객 동시 요청={}, 성공={}, 거절={}", threadCount, successCount.get(), rejectedCount.get());

        // 1인 1장 불변식. issued_coupon 의 (coupon_policy_id, member_id) 유니크 제약이 backstop 으로 동작한다.
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(issuedCouponRepository.count()).isEqualTo(1);
    }

    /**
     * 선착순 {@code totalQuantity} 장짜리 ACTIVE 쿠폰 정책을 구성하고 {@code couponPolicyId} 에 저장한다.
     */
    private void prepareCouponPolicy(int totalQuantity) {
        LocalDateTime now = LocalDateTime.now();
        CouponPolicy policy = couponPolicyRepository.save(new CouponPolicy(
                "동시성 테스트 쿠폰",
                DiscountType.FIXED_AMOUNT,
                1000L,
                null,
                now.minusHours(1),
                now.plusHours(1),
                totalQuantity));
        this.couponPolicyId = policy.getId();
    }

    private List<Long> createMembers(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Member member = memberRepository.save(
                    Member.create("coupon-buyer" + i + "@example.com", "password123!", "구매자" + i));
            ids.add(member.getId());
        }
        return ids;
    }

    /**
     * {@code threadCount} 개의 스레드를 동시에 출발시켜 {@code task} 를 실행한다.
     * 비즈니스 예외(중복/소진 등)와 DB 제약 위반은 거절로 집계한다.
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
                    // 비즈니스 예외(COUPON_002/003) 및 유니크 제약 위반(중복 발급)은 정상적인 거절로 본다.
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
        issuedCouponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
        memberRepository.deleteAll();
    }
}
