package com.elevenst.realtimechat.global.lock;

import com.elevenst.realtimechat.global.support.LockManager;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 분산 락을 적용하지 않는 No-Op LockManager. <b>Before 측정 전용 — 운영(local/prod)에서 절대 사용 금지.</b>
 *
 * <p>부하 테스트의 "Before(락 미적용)" 기준선을 실제 MySQL 환경에서 재현하기 위한 <b>측정 전용</b> 구현체다.
 * Policies 「부하 테스트 및 목표 성능 지표 정책」의 Before/After 기록 정책(락이 없는 상태에서 초과 판매·데드락을
 * 측정)을 위해서만 사용한다. {@code nolock} 프로파일에서만 활성화되며({@code SPRING_PROFILES_ACTIVE=local,nolock}),
 * local/prod 기본 실행에는 절대 포함되지 않는다(운영은 항상 {@link RedissonLockManager}).
 *
 * <p><b>경고</b>: 동시성 보호가 사라지므로 운영에서 활성화하면 초과 판매·중복 발급이 발생한다.
 * 보고서 `docs/performance/concurrency-load-report.md`의 Before 측정 외 용도로 사용하지 말 것.
 */
@Slf4j
@Component
@Profile("nolock")
public class NoOpLockManager implements LockManager {

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        // 측정 전용: 락을 걸지 않고 항상 획득 성공으로 처리한다(직렬화 없음).
        return true;
    }

    @Override
    public void unlock(String key) {
        // 분산 락 미적용: 해제할 락이 없다.
    }
}
