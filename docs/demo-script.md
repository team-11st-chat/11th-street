# 필수 기능 시연 스크립트

이 문서는 로컬 서버에서 필수 기능을 순서대로 시연하기 위한 절차입니다. PowerShell과 `curl.exe` 기준으로 작성했으며, 애플리케이션은 `http://localhost:8080`에서 실행 중이라고 가정합니다.

## 0. 사전 조건

- `docker compose up -d`로 MySQL과 Redis가 실행 중이어야 합니다.
- README의 환경변수를 설정한 뒤 `.\gradlew.bat bootRun`으로 서버를 실행합니다.
- 로컬 DB가 이전 시연 데이터로 오염된 경우 `docker compose down -v` 후 다시 시작합니다.
- 현재 회원 가입 API는 기본 역할을 `BUYER`로 생성합니다. 상품 등록에는 `SELLER`, 쿠폰 정책 등록에는 `SUPER_ADMIN` 권한이 필요하므로 시연 중 역할 변경 SQL을 사용합니다.
- 로컬 실행 profile에서는 쿠폰 발급과 타임세일 주문의 분산 락을 `RedissonLockManager`가 처리합니다. 동시성 테스트에서는 `LockManager`를 Mockito 목으로 대체하고, 부하 측정 전용 `nolock` 프로파일에서는 `NoOpLockManager`로 대체됩니다.
- 멱등성 처리는 현재 `FakeIdempotencyManager`를 사용하므로, Redis 기반 실제 멱등성 구현체 교체 후 중복 요청 시나리오를 다시 검증해야 합니다.

## 1. Health Check

```powershell
curl.exe http://localhost:8080/health
```

기대 결과: `status`가 `UP`으로 응답합니다.

## 2. 회원 생성

```powershell
$hostUrl = "http://localhost:8080"

$seller = curl.exe -s -X POST "$hostUrl/api/v1/members" `
  -H "Content-Type: application/json" `
  -d '{"email":"seller01@example.com","password":"plainPassword1","name":"seller01"}' | ConvertFrom-Json

$admin = curl.exe -s -X POST "$hostUrl/api/v1/members" `
  -H "Content-Type: application/json" `
  -d '{"email":"admin01@example.com","password":"plainPassword1","name":"admin01"}' | ConvertFrom-Json

$buyer = curl.exe -s -X POST "$hostUrl/api/v1/members" `
  -H "Content-Type: application/json" `
  -d '{"email":"buyer01@example.com","password":"plainPassword1","name":"buyer01"}' | ConvertFrom-Json
```

기대 결과: 각 요청이 `201 Created`로 처리되고 회원 ID가 응답됩니다.

## 3. 시연 권한과 카테고리 준비

현재 역할 변경과 카테고리 등록 API는 시연 범위에 없으므로 DB에 직접 준비합니다.

```powershell
docker exec -i 11th-street-local-mysql-1 mysql -urealtime_chat -prealtime_chat realtime_chat -e "UPDATE member SET role='SELLER' WHERE email='seller01@example.com';"
docker exec -i 11th-street-local-mysql-1 mysql -urealtime_chat -prealtime_chat realtime_chat -e "UPDATE member SET role='SUPER_ADMIN' WHERE email='admin01@example.com';"
docker exec -i 11th-street-local-mysql-1 mysql -urealtime_chat -prealtime_chat realtime_chat -e "INSERT INTO category(parent_id, name, depth, sort_order, created_at, updated_at) VALUES (NULL, '디지털', 1, 1, NOW(), NOW());"
docker exec -i 11th-street-local-mysql-1 mysql -urealtime_chat -prealtime_chat realtime_chat -e "INSERT INTO category(parent_id, name, depth, sort_order, created_at, updated_at) SELECT id, '이어폰', 2, 1, NOW(), NOW() FROM category WHERE name='디지털' AND depth=1;"
```

기대 결과: `seller01@example.com`은 `SELLER`, `admin01@example.com`은 `SUPER_ADMIN`이 되고, 하위 카테고리 `이어폰`이 생성됩니다.

## 4. 로그인과 토큰 저장

```powershell
$sellerLogin = curl.exe -s -X POST "$hostUrl/api/v1/auth/login" `
  -H "Content-Type: application/json" `
  -d '{"email":"seller01@example.com","password":"plainPassword1"}' | ConvertFrom-Json
$sellerToken = $sellerLogin.data.accessToken

$adminLogin = curl.exe -s -X POST "$hostUrl/api/v1/auth/login" `
  -H "Content-Type: application/json" `
  -d '{"email":"admin01@example.com","password":"plainPassword1"}' | ConvertFrom-Json
$adminToken = $adminLogin.data.accessToken

$buyerLogin = curl.exe -s -X POST "$hostUrl/api/v1/auth/login" `
  -H "Content-Type: application/json" `
  -d '{"email":"buyer01@example.com","password":"plainPassword1"}' | ConvertFrom-Json
$buyerToken = $buyerLogin.data.accessToken
```

기대 결과: 각 응답의 `data.accessToken`이 발급됩니다.

## 5. 상품 등록

```powershell
$categoryId = (docker exec -i 11th-street-local-mysql-1 mysql -N -urealtime_chat -prealtime_chat realtime_chat -e "SELECT id FROM category WHERE name='이어폰' AND depth=2 LIMIT 1;").Trim()

$product = curl.exe -s -X POST "$hostUrl/api/v1/products" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $sellerToken" `
  -d "{`"name`":`"무선 이어폰 Pro`",`"categoryId`":$categoryId,`"price`":89000,`"stockQuantity`":100}" | ConvertFrom-Json

$productId = $product.data.id
```

기대 결과: 상품이 `201 Created`로 등록되고 `productId`를 확보합니다.

## 6. 상품 검색과 인기 검색어 조회

```powershell
curl.exe "$hostUrl/api/v1/products?keyword=이어폰&page=0&size=20" `
  -H "Authorization: Bearer $buyerToken" `
  -H "Request-Guest-ID: demo-guest-001"

curl.exe "$hostUrl/api/v1/popular-keywords" `
  -H "Authorization: Bearer $buyerToken"
```

기대 결과: 상품 검색 응답에 등록한 상품이 포함되고, 인기 검색어 응답에서 검색어 집계 흐름을 확인할 수 있습니다.

## 7. 타임세일 등록과 주문

```powershell
$timeSale = curl.exe -s -X POST "$hostUrl/api/v1/timesales" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $sellerToken" `
  -d "{`"productId`":$productId,`"salePrice`":69000,`"startedAt`":`"2026-06-01T00:00:00`",`"endedAt`":`"2026-12-31T23:59:59`",`"initialQuantity`":10}" | ConvertFrom-Json

$timeSaleId = $timeSale.data.id

curl.exe -X POST "$hostUrl/api/v1/timesales/$timeSaleId/orders" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $buyerToken" `
  -H "Request-ID: demo-order-0001" `
  -d '{"quantity":1}'
```

기대 결과: 타임세일이 등록되고 구매자 주문이 `201 Created`로 처리됩니다.

## 8. 쿠폰 정책 등록과 발급

```powershell
$coupon = curl.exe -s -X POST "$hostUrl/api/v1/coupons" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $adminToken" `
  -d '{"name":"여름맞이 10% 할인","discountType":"PERCENTAGE","discountValue":10,"maxDiscountAmount":5000,"issueStartsAt":"2026-06-01T00:00:00","issueEndsAt":"2026-12-31T23:59:59","totalQuantity":100}' | ConvertFrom-Json

$couponPolicyId = $coupon.data.id

curl.exe -X POST "$hostUrl/api/v1/coupons/$couponPolicyId/issue" `
  -H "Authorization: Bearer $buyerToken" `
  -H "Request-ID: demo-coupon-0001"
```

기대 결과: 쿠폰 정책이 등록되고 구매자에게 쿠폰이 `201 Created`로 발급됩니다.

## 9. 시연 후 확인할 제한 사항

- 역할 변경과 카테고리 등록은 현재 Issue 범위가 아니므로 SQL로 준비했습니다.
- Redis Lock 적용 전/후 동시성 비교는 실제 Redis Lock 구현체 교체 후 별도로 검증해야 합니다.
- 검색 성능 Baseline/Local Cache/Remote Cache 비교와 대량 데이터 인덱스 비교는 각 성능 Issue의 산출물을 기준으로 별도 시연합니다.
- WebSocket/STOMP와 Redis Pub/Sub 채팅 시연은 채팅 고도화 Issue 완료 후 최종 시연에 포함합니다.

