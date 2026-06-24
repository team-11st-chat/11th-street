package com.elevenst.realtimechat.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.global.exception.ServiceException;
import com.elevenst.realtimechat.member.dto.MemberCreateRequest;
import com.elevenst.realtimechat.member.exception.MemberErrorCode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import com.elevenst.realtimechat.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest
class MemberServiceConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceConcurrencyTest.class);

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    private final String testEmail = "concurrent@example.com";

    @AfterEach
    void tearDown() {
        memberRepository.findByEmail(testEmail).ifPresent(member -> {
            memberRepository.delete(member);
            log.info("테스트 종료: 테스트용 멤버({}) 삭제 완료", testEmail);
        });
    }

    @Test
    @DisplayName("동시 회원가입 시 DB의 유니크 제약 조건으로 인해 하나만 성공하고 나머지는 409 예외가 발생한다")
    void testConcurrentSignup() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicatedExceptionCount = new AtomicInteger(0);

        MemberCreateRequest request = new MemberCreateRequest(
                testEmail,
                "password123!",
                "동시가입자"
        );

        log.info("--- 동시성 테스트 시작: {}개의 스레드 생성 ---", threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i + 1;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 모든 스레드가 여기서 동시 출발 대기
                    
                    memberService.signup(request);
                    successCount.incrementAndGet();
                    log.info("[스레드-{}] 회원가입 성공!", threadNumber);
                    
                } catch (ServiceException e) {
                    if (e.getErrorCode() == MemberErrorCode.EMAIL_DUPLICATED) {
                        duplicatedExceptionCount.incrementAndGet();
                        log.warn("[스레드-{}] 예외 발생: {}", threadNumber, e.getErrorCode().getMessage());
                    }
                } catch (Exception e) {
                    log.error("[스레드-{}] 예상치 못한 에러: ", threadNumber, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(); // 모든 스레드가 준비될 때까지 대기
        log.info("모든 스레드 준비 완료. 동시에 출발합니다!");
        startLatch.countDown(); // 락 해제 (동시 출발)
        
        doneLatch.await(); // 모든 스레드 작업 완료 대기
        
        log.info("--- 테스트 종료 ---");
        log.info("성공 횟수: {}", successCount.get());
        log.info("중복 예외 발생 횟수: {}", duplicatedExceptionCount.get());

        assertThat(successCount.get()).isEqualTo(1); // 1개의 요청만 성공해야 함
        assertThat(duplicatedExceptionCount.get()).isEqualTo(threadCount - 1); // 나머지 요청은 모두 409 중복 예외 발생
    }
}
