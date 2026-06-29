# 동시성 부하 결과와 트러블슈팅 보고서

> 상위 추적 이슈: [#6](https://github.com/team-11st-chat/11th-street/issues/6) · 실행 이슈: [#33](https://github.com/team-11st-chat/11th-street/issues/33)
> 선행 작업: [#31](https://github.com/team-11st-chat/11th-street/issues/31)(k6 스크립트), [#32](https://github.com/team-11st-chat/11th-street/issues/32)(Request-ID·Fail-Closed·Lock 검증)
> 근거: [wiki/MVP#작업-b](https://github.com/team-11st-chat/11th-street/wiki/MVP#작업-b--동시성-고도화부하-검증), [wiki/Policies#부하-테스트-및-목표-성능-지표-정책](https://github.com/team-11st-chat/11th-street/wiki/Policies#부하-테스트-및-목표-성능-지표-정책)

이 문서는 MVP 3단계 작업 B(동시성 고도화·부하 검증)에서 수행한 타임세일 주문·선착순 쿠폰 발급 부하 테스트의 **정식 규모(동시 요청 500·1,000건) Before/After 측정 결과, 정합성 검증식 충족 여부, 발견한 병목과 개선 과정, Policies 확정 목표 대비 평가**를 정리한다. 부하 스크립트·실행 절차·락 설계 근거는 아래 문서를 참조한다.

- 실행 절차: [docs/performance/k6/empty-db-runbook.md](k6/empty-db-runbook.md)
- 스크립트와 수집 지표: [docs/performance/k6/README.md](k6/README.md)
- 락 방식 선택 근거: [docs/lock-strategy-comparison.md](../lock-strategy-comparison.md)

---

## 1. 측정 결과 (성공 수 · 실패 수 · 응답 시간 · Lock 실패 수)

이 절은 **선착순 한정 수량 M=100건**에 **순간 동시 요청 N=500·1,000건**을 유입시킨 정식 규모 측정 결과다. Policies 「Before/After 데이터 기록 정책」에 따라 **Before(분산 락 미적용)**와 **After(분산 락 적용)**를 같은 환경에서 측정했다.

> 측정 환경(중요): 단일 로컬 머신(Windows)에서 애플리케이션 서버·k6 부하 생성기·MySQL·Redis를 함께 실행했다. 따라서 **절대 응답 시간은 운영 환경 대표값이 아니라 로컬 지표**이며, 정합성(초과 판매 0)·상대 비교·병목 양상이 핵심 산출물이다. 구매자 토큰 1,000개를 시드하고 1요청=1구매자(중복 회원 거절 회피)로 실행했다.

### After — 분산 락 적용 (현재 코드, `local` 프로파일)

| 엔드포인트 | 동시 N | 한정 수량 | 성공 | 품절 거절 | Lock 실패(503) | p95(ms) | 초과 판매(DB 검증) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 타임세일 주문 | 500 | 100 | 100 | 269 | 131 | 5,737 | 0 (remaining=0) |
| 타임세일 주문 | 1,000 | 100 | 100 | 686 | 214 | 10,247 | 0 (remaining=0) |
| 쿠폰 발급 | 500 | 100 | 100 | 297 | 103 | 4,374 | 0 (remaining=0) |
| 쿠폰 발급 | 1,000 | 100 | 100 | 768 | 132 | 8,336 | 0 (remaining=0) |

- **정합성(초과 판매 0)**: 모든 시나리오에서 성공 수가 한정 수량 100과 정확히 일치했고, DB에서 `COMPLETED/ISSUED` 행 수=100, `remaining_quantity=0`을 교차 확인했다. 분산 락이 임계구역을 직렬화해 초과 판매·발급을 0건으로 막았다.
- **Lock 실패(503)**: 동시성이 커질수록 증가했다(타임세일 500→1,000: 131→214). 단일 락을 3초 안에 얻지 못한 요청이 Policies Fail-Closed 정책대로 503으로 즉시 거절된 것으로, 정합성과 무관한 가용성 지표다(§4 참고).
- **응답 시간**: p95가 N=1,000에서 8~10초로 Policies 합격 기준 **평균 500ms를 크게 초과**했다. 주원인은 (a) 단일 락 직렬화에 따른 처리량 상한(임계구역 ~50ms → 초당 ~20건), (b) 단일 머신에서 Tomcat 워커(기본 200) 한도를 넘는 1,000 동시 연결의 큐잉이다(병목 분석 §3-3).
- **근거 파일(커밋됨)**: 위 표의 성공/실패/503/p95 원시 수치는 각 실행의 k6 요약에 들어 있다 — [after-timesale-n500](k6/results/after-timesale-n500/timesale-order-summary.md), [after-timesale-n1000](k6/results/after-timesale-n1000/timesale-order-summary.md), [after-coupon-n500](k6/results/after-coupon-n500/coupon-issue-summary.md), [after-coupon-n1000](k6/results/after-coupon-n1000/coupon-issue-summary.md). 각 디렉터리에 `.md`(요약)와 `.json`(전체 메트릭)이 함께 커밋되어 있어 표의 수치를 그대로 재검증할 수 있다.

### Before — 분산 락 미적용 (`nolock` 프로파일, 측정 전용)

| 엔드포인트 | 동시 N | 한정 수량 | 성공 | 실패(HTTP 500 데드락) | DB 정합성 |
| --- | ---: | ---: | ---: | ---: | --- |
| 타임세일 주문 | 200 | 100 | 39 | 161 | 손상: 주문 39건인데 remaining=82 (정합이면 61) |
| 쿠폰 발급 | 200 | 100 | 35 | 165 | 손상(동일 패턴) |
| 타임세일 주문 | 1,000 | 100 | 미완주 | 커넥션 풀 고갈 → 연결 거부/타임아웃 | 측정 중단 |

- 락을 제거하면 동시 요청이 직렬화되지 않아, 같은 재고·상품 행에 대한 동시 UPDATE가 **InnoDB 데드락**을 유발한다. N=200에서 이미 **약 80%(161/200, 165/200) 요청이 HTTP 500 데드락으로 실패**했다.
- 성공한 소수(39·35건)조차 **재고 카운터가 주문 수와 불일치**했다(remaining=82 ≠ 100−39=61). read-modify-write의 lost update로 재고가 실제보다 많게 남아 **초과 판매를 허용하는 손상 상태**다.
- N=1,000에서는 1,000 트랜잭션이 동시에 DB 커넥션 풀(기본 10)을 점유하려다 **풀 고갈 → 신규 연결 거부/타임아웃**으로 측정을 완주하지 못했다. 역설적으로 **분산 락이 DB 부하를 직렬화해 풀 고갈을 막아주고 있었음**을 보여준다.
- **근거 파일(커밋됨)**: [before-timesale-n200](k6/results/before-timesale-n200/timesale-order-summary.md), [before-coupon-n200](k6/results/before-coupon-n200/coupon-issue-summary.md). 단, **k6 요약은 성공/실패/503/p95만 기록**하므로, 실패의 내역(HTTP 500 데드락)과 재고 카운터 손상은 k6 요약이 아니라 **앱 로그·DB 검증(§3-3)**에서 확인하며 §5 절차로 재현한다. (그래서 위 표의 `실패` 161·165건은 k6 요약상 단순 실패 수이고, 그 전부가 데드락 500이라는 사실은 앱 로그 근거다.)

> 참고: 위 측정의 k6 요약은 재검증을 위해 `results/<run-id>/`에 커밋했다(`.md`=요약 수치, `.json`=전체 메트릭). smoke(10/10) 초기 결과도 [timesale-order-summary.md](k6/results/timesale-order-summary.md)·[coupon-issue-summary.md](k6/results/coupon-issue-summary.md)에 있다. `results/.gitignore`는 즉석·로컬 재실행 산출물을 기본 무시하므로 **커밋된 근거 파일만 추적**되며(즉석 재실행 결과는 추적되지 않음), `results/debug-*`는 초기 검증용 디버그 실행으로 회귀 기준값이 아니다.

---

## 2. 정합성 검증식 충족 여부

정합성은 응답 시간과 분리해 **하드 임계(threshold)** 로 강제한다. 임계를 위반하면 k6가 비정상 종료한다.

| 검증식 | 의미 | After(락 적용) | Before(락 미적용) |
| --- | --- | --- | --- |
| `성공 수 <= 한정 수량` | 초과 판매·발급 0건 | **충족** — N=500·1,000 모두 100 ≤ 100, DB `remaining_quantity=0` | **위반** — 데드락·lost update로 재고 카운터 손상(remaining ≠ 초기−성공) |
| `duplicate_successes == 0` | 동일 Request-ID 중복 성공 0건 | 충족 (0) | 해당 없음(probe 미실행) |
| 응답에 `data.id` 존재 | 성공 응답이 실제 생성 자원을 반환 | 충족 | 충족(성공분) |

- **초과 판매/발급 방지(After)**: 정책 "초과 판매 및 중복 발급율 0%"를 `성공 수 ≤ 한정 수량`으로 강제한다. N=500·1,000 모두 성공 수=한정 수량(100)으로 정확히 일치했고, DB 교차 검증(`remaining_quantity=0`, 상태별 행 수=성공 수)으로 0건 초과를 확인했다.
- **락 제거 시 정합성 붕괴(Before)**: 동일 환경에서 분산 락만 제거하면 데드락·lost update로 정합성이 깨진다(§1 Before). 즉 **정합성 100%는 분산 락에 의존**한다.
- **중복 방지의 범위**: 동일 Request-ID 멱등성의 정밀 검증은 k6 외부 관측이 아니라 [#32](https://github.com/team-11st-chat/11th-street/issues/32)의 facade 단위 검증(`CouponIssueFacadeTest`, `TimeSaleOrderFacadeTest`)이 담당한다. k6 probe는 "동일 요청이 두 번 성공하지 않는다"만 외부에서 재확인하는 보조 수단이다(README §Scope Notes).

---

## 3. 발견한 병목과 개선 과정

이 절은 (a) 락 설계 단계에서 식별·차단한 구조적 병목, (b) 측정 절차에서 드러난 운영 이슈, (c) 정식 규모(500~1,000) 측정에서 실제 관측한 병목을 구분해 정리한다.

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
- **임계구역 길이 vs Lease**: 임계구역이 Lease(2초)보다 길어지면 락이 먼저 풀려 보호가 깨진다. → 임계구역을 수량 차감·생성으로 최소화하는 원칙을 유지한다.

### 3-3. 정식 규모(500~1,000) 측정에서 관측한 병목

| 관측 | 수치 | 해석 | 후속 |
| --- | --- | --- | --- |
| 응답 시간 목표 미달 | p95 5.7~10.2s (목표 평균 500ms) | 단일 락 직렬화 처리량 상한(임계구역 ~50ms → 초당 ~20건) + 단일 머신 Tomcat 200 워커에 1,000 연결 큐잉 | 부하 생성기·서버 분리 환경에서 재측정, 락 키 샤딩·임계구역 단축 검토 |
| Lock 503 증가 | N 500→1,000에서 131→214(타임세일), 103→132(쿠폰) | 3초 Wait 안에 락을 못 얻은 요청의 Fail-Fast(정합성과 무관) | 가용성 허용치(503 비율) 정책 정의 필요(Policies 미규정, §4) |
| (Before) DB 데드락 폭주 | N=200에서 ~80% HTTP 500 | 락 없이 같은 재고/상품 행에 동시 UPDATE → InnoDB 데드락 | 분산 락 유지가 정합성+안정성의 전제임을 확인 |
| (Before) 커넥션 풀 고갈 | N=1,000 미완주 | 락 없는 1,000 동시 트랜잭션이 Hikari 풀(기본 10) 점유 경쟁 → 연결 거부 | 락이 DB 부하를 직렬화해 풀을 보호함을 확인 |

- 핵심: **분산 락은 정합성(초과 판매 0)뿐 아니라 DB 안정성(데드락·풀 고갈 방지)까지 제공**한다. 그 비용은 처리량 상한과 고동시성 구간의 503·응답 지연이며, 이는 Policies의 Fail-Closed(가용성보다 정합성 우선) 결정과 일치한다.
- 응답 시간 목표(평균 500ms)는 **단일 머신 로컬 측정에서는 미달**이다. 절대값은 측정 환경 영향이 크므로, 서버/부하 생성기를 분리한 환경에서의 재측정을 후속 과제로 남긴다.

---

## 4. 목표치와 Retry 범위 (Policies 확정값 채택)

> 이슈 [#6](https://github.com/team-11st-chat/11th-street/issues/6) 참고사항은 아래 목표치가 **MVP 위키 「작업 B — 동시성 고도화·부하 검증」의 `결정할 내용`으로 남아 있다**고 지적했다. 그러나 [Policies 위키 「부하 테스트 및 목표 성능 지표 정책」](https://github.com/team-11st-chat/11th-street/wiki/Policies#부하-테스트-및-목표-성능-지표-정책)과 [「Redis 분산 락을 이용한 동시성 제어 정책」](https://github.com/team-11st-chat/11th-street/wiki/Policies#redis-분산-락을-이용한-동시성-제어-정책)은 해당 값을 **상태: 확정(2026-06-22)** 으로 기재한다. Policies가 정책 기준 문서이므로, 본 보고서는 **Policies 확정값을 공식 목표로 채택**한다. MVP 위키의 `결정할 내용` 문구는 Policies 관리규칙(미결정 문구 취소선 보존 + 확정 정책 연결)에 따라 정리 대상이다. 이 확정 목표에 대한 정식 규모(500·1,000) 측정 결과는 §1·§3-3에 있다.

| 항목 | Policies 확정값 | 본 보고서 채택 / 측정 결과 |
| --- | --- | --- |
| 목표 동시 요청 규모 | 순간 동시 요청 500~1,000건 | **확정 채택**. 정식 규모(500·1,000) 측정 완료(§1) |
| 허용 응답 시간 | 평균 500ms 이내 | **확정 채택**(합격 기준 평균 500ms). 정식 규모 측정에서 p95 5.7~10.2s로 **목표 미달** — 단일 머신 한계 포함, 분리 환경 재측정 후속(§3-3) |
| 허용 실패율(정합성) | 초과 판매·중복 발급율 0% | **확정 채택**. N=500·1,000 After에서 충족(성공=한정 수량 100, DB `remaining=0`); Before는 위반(§1·§2) |
| Lock 획득 대기(Wait) | 3초, 초과 시 Fail-Fast 반환 | **확정 채택**. 구현값 3초와 일치 |
| Lock 임계 만료(Lease) | 2초 | **확정 채택**. 구현값 2초와 일치 |
| Redis 장애 처리 | Fail-Closed(즉시 거절) | **확정 채택**. 구현과 일치 |
| Retry 범위 | 3초 내 미획득 시 즉시 실패(재시도 없음), Redis 장애 시 즉시 거절 | **확정 채택** — 클라이언트/서버 재시도 없음(Fail-Closed). 구현과 일치 |

**정식 측정 시 적용할 합격 기준 (위 Policies 확정값 기준)**

1. **부하 규모**: 순간 동시 요청 500~1,000건. VUs·ramp 등 실행 파라미터는 런북에서 이 규모에 맞춰 설정한다.
2. **응답 시간**: 평균 500ms 이내를 합격 임계로 적용한다. p95는 보조 지표로 수집한다.
3. **정합성**: 초과 판매·중복 발급 0%를 하드 임계(threshold)로 강제한다(§2).
4. **Retry**: Fail-Closed(재시도 없음)를 유지한다.

> 참고: Policies는 정합성 허용 실패율(0%)만 목표 수치로 규정하고, 락 실패(503)에 대한 별도 가용성 허용 수치는 두지 않는다(대신 Wait 3초 초과 시 Fail-Fast, Redis 장애 시 Fail-Closed 동작으로 규정한다). 따라서 503 비율은 합격 임계가 아니라 가용성 관찰용 참고 지표로 수집한다.

---

## 5. 측정 재현 방법

본 측정은 다음 절차로 수행했다(상세 환경 변수·DB 검증 SQL은 [empty-db-runbook.md](k6/empty-db-runbook.md)).

1. 로컬 인프라(`docker compose up -d`)와 앱(`bootRun`, `local` 프로파일=분산 락 적용)을 기동한다.
2. 구매자 1,000명·판매자·관리자·상품(재고 5,000)을 시드하고 구매자 토큰 1,000개를 생성한다.
3. 실행마다 **새 타임세일/쿠폰(한정 수량 100)**을 생성하고, `VUS=REQUESTS=N`(N=500·1,000)으로 k6를 실행한다. 토큰은 VU별 1:1 매핑이라 중복 회원 거절을 피한다(스크립트의 `tokens[(__VU-1) % length]`).
4. **After**는 위 그대로(락 적용). **Before**는 동일 환경에서 `SPRING_PROFILES_ACTIVE=local,nolock`으로 앱을 재기동해 분산 락만 제거(`NoOpLockManager`)하고 같은 절차로 측정한다.
5. 각 실행 후 DB 검증 SQL로 `remaining_quantity`와 상태별 행 수=성공 수를 교차 확인한다.

> `nolock` 프로파일은 Before 기준선(락 미적용) 재현을 위한 **측정 전용**이며,
> `local` 기본 실행에는 포함되지 않는다. `prod`와 `nolock`이 함께 활성화되면
> 운영 안전을 위해 `RedissonLockManager`가 유지된다.
> 코드: `global/lock/NoOpLockManager.java`(`@Profile("nolock & !prod")`),
> `RedissonLockManager`(`@Profile("!nolock | prod")`).

> 단일 머신 한계: 본 측정은 서버·부하 생성기·DB를 한 머신에서 실행했다. 절대 응답 시간(평균 500ms 목표 대비)은 분리 환경에서 재측정이 필요하며, 정합성(초과 판매 0)과 Before/After 대비는 환경과 무관하게 유효하다.
