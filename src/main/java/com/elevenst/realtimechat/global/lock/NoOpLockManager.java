package com.elevenst.realtimechat.global.lock;

import com.elevenst.realtimechat.global.support.LockManager;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 분산 락을 적용하지 않는 No-Op LockManager.
 *
 * <p>부하 테스트의 "Before(락 미적용)" 기준선을 실제 MySQL 환경에서 재현하기 위한 측정 전용 구현체다.
 * Policies 「부하 테스트 및 목표 성능 지표 정책」의 Before/After 기록 정책(락이 없는 상태에서 초과 판매를
 * 측정)을 위해서만 사용한다. {@code nolock} 프로파일에서만 활성화되며, local/prod 기본 실행에는 절대
 * 포함되지 않는다(운영은 항상 {@link RedissonLockManager}).
 */
@Slf4j
@Component
@Profile("nolock")
public class NoOpLockManager implements LockManager {

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        return true;
    }

    @Override
    public void unlock(String key) {
        // 분산 락 미적용: 해제할 락이 없다.
    }
}
