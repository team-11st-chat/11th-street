# Issue #29 — 쿠폰 발급 Redis Lock 보호 적용 (검증/추적)

> 상위 추적 이슈: #5 · 근거: [wiki/MVP#쿠폰-발급-원자적-보호-범위](https://github.com/team-11st-chat/11th-street/wiki/MVP#쿠폰-발급-원자적-보호-범위)

이 문서는 이슈 #29의 책임 범위(쿠폰 발급 분산 락 보호)가 어디에 구현되어 있는지 추적성을 기록한다.
핵심 구현은 별도 코드 중복 없이 이미 `develop` 에 병합되어 있으며(아래 커밋), 본 브랜치는 추적·검증 용도다.

## 1. 구현 범위와 코드 매핑

| 이슈 #29 세부 작업 | 구현 위치 |
| --- | --- |
| `lock:coupon:{couponPolicyId}` 키 적용 | `CouponIssueFacade#issueCoupon` (`lockKey = "lock:coupon:" + couponPolicyId`) |
| 발급 원자적 보호 범위(기간·중복·잔여 수량 차감·고객 쿠폰 생성) | `CouponIssueService#issueCoupon` 의 `@Transactional` 흐름 전체가 락 임계영역 안에서 실행 |
| 동일 고객 중복 발급 방지 | `IssuedCouponRepository#existsByCouponPolicyIdAndMemberId` + `issued_coupon` (coupon_policy_id, member_id) 유니크 제약(backstop) |
| 전체 수량 초과 방지 | `CouponPolicy#issue` 의 `remainingQuantity <= 0` 검증 + 락 직렬화 (엔티티에 낙관적 락 없음 → 분산 락이 유일 보호 장치) |

락 라이프사이클은 트랜잭션 **바깥**(Facade)에 두어, 트랜잭션 커밋이 끝난 뒤 `finally` 에서 해제한다.
이로써 락 해제가 커밋보다 먼저 일어나 다음 대기 요청이 미커밋 상태를 보고 검증을 우회하는 문제를 차단한다.

관련 커밋(이미 `develop` 병합):
- `53666a2` feat: 쿠폰 정책 등록 및 선착순 발급 API 구현
- `6bc1770` refactor: 쿠폰 발급 분산 락을 트랜잭션 바깥 Facade로 분리

## 2. Stub/Fake 의존성

| 대상 의존성 | 처리 | 클래스 | 교체 시점 |
| --- | --- | --- | --- |
| 분산 락 (운영) | 실제 구현 | `RedissonLockManager` (`@Primary @Profile("!test")`) | — (구현 완료) |
| 분산 락 (테스트/로컬) | Fake(no-op) | `FakeLockManager` (`@Profile("test")`) | 통합 테스트에서 실제 Redis 사용 시 |
| 멱등성(Request-ID) | Fake(항상 true) | `FakeIdempotencyManager` (프로파일 무제한) | 멱등성 이슈에서 Redis 기반 실제 구현으로 교체 예정 |

> 멱등성 Fake 는 이슈 #29 범위가 아니며 별도 이슈에서 실제 구현체로 교체된다.

## 3. 테스트 미작성 사유

이번 작업 합의에 따라 신규 테스트를 추가하지 않았다. 동시성 재현 테스트
`CouponIssueConcurrencyTest#couponOverIssuingReproduced` 는 `@Disabled` 상태이며,
`test` 프로파일에서는 `FakeLockManager`(no-op) 가 주입되어 실제 직렬화가 일어나지 않으므로
이 테스트의 GREEN 전환은 통합 테스트 인프라(실제 Redis/Redisson)가 갖춰진 뒤에 검증한다.

## 4. 추후 테스트 시나리오

- **정상 흐름**: 잔여 수량 N, 동시 요청 M(>N) → 정확히 N건만 성공, `remainingQuantity == 0`.
- **예외 흐름**: 발급 기간 외(`COUPON_001`), 수량 소진(`COUPON_002`), 중복 발급(`COUPON_003`), 락 획득 실패 시 503(Fail-Closed).
- **상태 변경 검증**: 발급 후 `remainingQuantity` 단조 감소, `0` 도달 시 상태 `ENDED` 전환.
- **외부 의존성 연동 검증**: `RedissonLockManager` 가 `lock:coupon:{id}` 키로 직렬화(Wait 3s/Lease 2s), 커밋 후 해제.

## 5. 검증 결과

- 실행: `./gradlew compileJava` → **성공 (EXIT 0)**
- 미실행: 동시성/통합 테스트 — 실제 Redis 인프라 미연동 + 이번 작업 테스트 미작성 합의에 따라 보류.
