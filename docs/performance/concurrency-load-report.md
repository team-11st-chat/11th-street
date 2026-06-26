# 동시성 부하 결과와 트러블슈팅 보고서

> 상위 추적 이슈: [#6](https://github.com/team-11st-chat/11th-street/issues/6) · 실행 이슈: [#33](https://github.com/team-11st-chat/11th-street/issues/33)
> 선행 작업: [#31](https://github.com/team-11st-chat/11th-street/issues/31)(k6 스크립트), [#32](https://github.com/team-11st-chat/11th-street/issues/32)(Request-ID·Fail-Closed·Lock 검증)
> 근거: [wiki/MVP#작업-b](https://github.com/team-11st-chat/11th-street/wiki/MVP#작업-b--동시성-고도화부하-검증), [wiki/Policies#부하-테스트-및-목표-성능-지표-정책](https://github.com/team-11st-chat/11th-street/wiki/Policies#부하-테스트-및-목표-성능-지표-정책)

이 문서는 MVP 3단계 작업 B(동시성 고도화·부하 검증)에서 수행한 타임세일 주문·선착순 쿠폰 발급 부하 테스트의 **측정 결과, 정합성 검증식 충족 여부, 발견한 병목과 개선 과정, 미확정으로 남은 목표치**를 정리한다. 부하 스크립트·실행 절차·락 설계 근거는 아래 문서를 참조한다.

- 실행 절차: [docs/performance/k6/empty-db-runbook.md](k6/empty-db-runbook.md)
- 스크립트와 수집 지표: [docs/performance/k6/README.md](k6/README.md)
- 락 방식 선택 근거: [docs/lock-strategy-comparison.md](../lock-strategy-comparison.md)

---

## 1. 측정 결과 (성공 수 · 실패 수 · 응답 시간 · Lock 실패 수)

아래 값은 저장소에 커밋된 k6 요약 결과([results/](k6/results/))에서 수집한 값이다. 현재 커밋된 결과는 로컬 smoke 규모(VUs 10, 요청 10)의 실행이며, 정책 목표인 **500~1,000건 동시 요청 규모의 측정은 아직 수행되지 않았다**(아래 [4. 확인 필요](#4-확인-필요로-남은-목표치와-retry-범위) 참조).

| 시나리오 | VUs / 요청 | 수량 상한 | 성공 | 실패 | 응답 p95 | Lock 실패(503) | 중복 성공 | 근거 파일 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 타임세일 주문 | 10 / 10 | 10 | 10 | 0 | 454.25ms | 0 | 0 | [timesale-order-summary.md](k6/results/timesale-order-summary.md) |
| 쿠폰 발급 | 10 / 10 | 10 | 10 | 0 | 307.85ms | 0 | 0 | [coupon-issue-summary.md](k6/results/coupon-issue-summary.md) |

- **성공 수**: 준비한 구매자 토큰 수(10)와 동일. 재고/수량 상한(10)을 정확히 소진했다.
- **실패 수**: 0. 정상 흐름에서 비즈니스 거절·락 실패가 발생하지 않았다.
- **응답 시간**: k6 커스텀 트렌드(`timesale_response_time`, `coupon_response_time`)의 p95를 기록한다. 통과/실패 임계로 사용하지 않고 수집만 한다(사유는 §4).
- **Lock 실패 수**: API가 `503 SERVICE_UNAVAILABLE`을 반환할 때 `*_lock_failures`로 집계한다. 정상 규모 실행에서는 락 경합·Redis 장애가 없어 0이며, k6는 한 번도 증가하지 않은 커스텀 카운터를 요약에서 생략한다.

> 참고: `results/debug-*` 하위 결과는 시나리오 검증 과정의 디버그 실행이다. `debug-repeat-*`의 "성공 0 / 실패 3"은 동일 구매자 토큰으로 재실행해 **이미 발급·구매한 회원**이 비즈니스 규칙으로 거절된 케이스이며(런북 §7), 초과 판매/발급 실패가 아니다. 회귀 기준값으로 사용하지 않는다.

---

## 2. 정합성 검증식 충족 여부

정합성은 응답 시간과 분리해 **하드 임계(threshold)** 로 강제한다. 임계를 위반하면 k6가 비정상 종료하므로, 아래 결과는 모두 통과 상태를 의미한다.

| 검증식 | 의미 | 타임세일 | 쿠폰 |
| --- | --- | --- | --- |
| `successful_orders \| successful_issues <= 수량 상한` | 초과 판매·초과 발급 0건 | 충족 (10 ≤ 10) | 충족 (10 ≤ 10) |
| `duplicate_successes == 0` | 동일 Request-ID 중복 성공 0건 | 충족 (0) | 충족 (0) |
| `duplicate_probe_failures == 0` | 중복 probe의 첫 요청은 성공해야 함 | probe 미실행 | probe 미실행 |
| 응답에 `data.id` 존재 | 성공 응답이 실제 생성 자원을 반환 | 충족 | 충족 |

- **초과 판매/발급 방지**: 정책 "초과 판매 및 중복 발급율 0%"를 검증식 `성공 수 ≤ 수량 상한`으로 강제하며, 커밋된 실행에서 충족했다.
- **중복 방지의 범위**: 커밋된 결과는 중복 Request-ID probe를 실행하지 않았다(`RUN_DUPLICATE_PROBE=false`). 동일 Request-ID 멱등성의 정밀 검증은 k6 외부 관측이 아니라 [#32](https://github.com/team-11st-chat/11th-street/issues/32)의 facade 단위 검증(`CouponIssueFacadeTest`, `TimeSaleOrderFacadeTest`)이 담당한다. k6 probe는 "동일 요청이 두 번 성공하지 않는다"만 외부에서 재확인하는 보조 수단이다(README §Scope Notes).
- **DB 정합성 교차 확인**: 위 검증식은 API 응답 기준이다. 실제 잔여 수량 차감·중복 행 미생성은 런북의 DB 검증 SQL(`remaining_quantity = 0`, 상태별 행 수 = 성공 수)로 추가 확인한다.

---

## 3. 발견한 병목과 개선 과정

현재 커밋된 측정은 smoke 규모라 **런타임 경합 병목이 관측될 조건이 아니다**. 따라서 이 절은 (a) 락 설계 단계에서 식별·차단한 구조적 병목과 (b) 측정 절차에서 드러난 운영 이슈를 구분해 정리한다.

### 3-1. 설계 단계에서 식별·차단한 구조적 병목

| 병목/위험 | 원인 | 대응(개선) | 근거 |
| --- | --- | --- | --- |
| read-modify-write 경합 | "잔여 조회 → 검증 → 차감"이 직렬화되지 않으면 초과 판매/발급 | 임계구역 전체를 Redisson 분산 락으로 직렬화 | [lock-strategy-comparison §1·6](../lock-strategy-comparison.md) |
| DB 커넥션 점유 | 비관적 락은 락 대기 = 커넥션 점유 → 풀 고갈 | 락 부하를 Redis로 분리, DB 커넥션은 임계구역에서만 사용 | lock-strategy §3·4 |
| 재시도 폭증 | 낙관적 락은 고경쟁에서 충돌·재시도가 throughput을 무너뜨림 | 사전 차단(대기) 방식의 분산 락 채택, 무한 재시도 대신 Wait 3초 후 Fail-Closed | lock-strategy §2·6 |
| 락 잔류(보유 중 프로세스 사망) | 분산 락은 해제 누락 시 영구 점유 | Lease Time 2초 자동 만료 | lock-strategy §6 |
| 미커밋 상태 우회 | 락을 커밋보다 먼저 해제하면 다음 대기자가 미커밋 수량을 보고 검증 우회 | 락 획득/해제를 `@Transactional` 바깥 Facade에 두어 **커밋 후 해제** | lock-strategy §6, `TimeSaleOrderFacade`·`CouponIssueFacade` |
| 남의 락 해제 | 만료 후 재획득된 락을 이전 소유자가 해제 | `isHeldByCurrentThread` 확인 후에만 unlock | `RedissonLockManager#unlock` |

### 3-2. 측정 절차에서 드러난 운영 이슈와 개선

- **반복 실행 시 거짓 실패**: 동일 구매자 토큰·동일 타임세일/쿠폰으로 재실행하면 "이미 구매/발급" 거절이 발생해 결과가 오염된다. → 런북 §7에 재실행 시 새 구매자·새 정책을 생성하도록 절차를 명시하고, 회귀 기준값과 디버그 실행(`debug-*`)을 분리했다.
- **중복 probe 관측의 한계**: probe가 동일 토큰·동일 Request-ID를 재사용하므로 외부 관측만으로는 "Request-ID 재사용 거절"과 "1인 1매 정책 거절"을 구분할 수 없다. → 정밀 멱등성 검증은 facade 단위 검증으로 이관(#32), k6는 보조 확인으로 한정(README §Scope Notes).
- **임계구역 길이 vs Lease**: 임계구역이 Lease(2초)보다 길어지면 락이 먼저 풀려 보호가 깨진다. → 임계구역을 수량 차감·생성으로 최소화하는 원칙을 유지한다. 대규모 실행에서 p95가 Lease에 근접하는지 추적이 필요하다(§4).

---

## 4. 확인 필요로 남은 목표치와 Retry 범위

> 이슈 [#6](https://github.com/team-11st-chat/11th-street/issues/6) 참고사항은 아래 목표치가 **MVP 위키에 `결정할 내용`으로 남아 있다**고 명시한다. 반면 **Policies 위키에는 확정값으로 기재**되어 있어 두 문서가 어긋난다. 본 보고서는 측정 비교 기준으로 Policies 값을 잠정 채택하되, 두 문서 정합 확정 전까지 **(확인 필요)** 로 표시한다.

| 항목 | Policies 위키 기재값 | MVP 위키 상태 | 본 보고서 처리 |
| --- | --- | --- | --- |
| 목표 동시 사용자 수 | 순간 동시 요청 500~1,000건 | 결정할 내용 | **(확인 필요)** — 잠정 목표 1,000건, 미측정 |
| 허용 응답 시간 | 선착순 평균 500ms 이내 | 결정할 내용 | **(확인 필요)** — smoke p95 454ms/308ms는 참고치, 임계 미적용 |
| 허용 실패율 | 초과 판매·중복 발급율 0% | 결정할 내용 | 정합성 0%는 검증식으로 강제(§2). 락 실패(503) 허용율은 **(확인 필요)** |
| Lock 획득 대기 시간 | 3초 | 결정할 내용 | 구현값 3초와 일치. 공식 목표 확정 **(확인 필요)** |
| Lock 임계(Lease) 만료 | 2초 | 결정할 내용 | 구현값 2초와 일치. 공식 목표 확정 **(확인 필요)** |
| Retry 허용 범위 | 3초 내 미획득 시 즉시 실패 반환(재시도 없음), Redis 장애 시 즉시 거절 | 결정할 내용 | 현재 구현은 **클라이언트/서버 재시도 없음(Fail-Closed)**. 재시도 허용 여부·횟수·백오프 정책 **(확인 필요)** |

**후속 측정 시 확정해야 할 값**

1. 정식 부하 규모(500/1,000)와 VUs·ramp 패턴.
2. 응답 시간 합격 임계(평균 500ms를 p95에도 적용할지, 별도 p95 목표를 둘지).
3. 락 실패(503) 허용 비율(정합성 0%와 별개로, 가용성 측면 허용치).
4. Retry 정책: Fail-Closed 유지 여부, 허용 시 클라이언트/서버 중 어디서·몇 회·어떤 백오프로 할지.

---

## 5. 측정 재현 방법

정식 규모(500~1,000) 측정은 런북 절차를 따라 `$buyerCount`, `initialQuantity`/`totalQuantity`, `VUS`, `REQUESTS`를 동일 값으로 맞춰 실행한다. 상세 환경 변수와 DB 검증 SQL은 [empty-db-runbook.md](k6/empty-db-runbook.md)를 참조한다. Redis 장애 Fail-Closed·Lock 만료 시나리오는 스크립트 실행 중 런타임 환경에서 장애를 주입해 `503` 집계를 관측한다(README §Scope Notes).
