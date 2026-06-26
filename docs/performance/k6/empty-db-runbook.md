# 빈 DB 기준 k6 부하 테스트 실행 절차

이 문서는 로컬 DB를 비어 있는 상태에서 시작해 타임세일 주문과 쿠폰 발급 k6 테스트를 재현하는 절차를 정리한다.

대상 스크립트:

- `docs/performance/k6/timesale-order-load.js`
- `docs/performance/k6/coupon-issue-load.js`

## 1. 실행 전 준비

필수 도구:

- Docker Desktop 또는 Docker Compose
- Java 21
- k6

k6가 설치되어있지 않다면 다음 명령어로 설치할 수 있습니다.
```powershell
winget install GrafanaLabs.k6
```

k6 명령이 PATH에 없으면 전체 경로로 실행한다.

```powershell
& "C:\Program Files\k6\k6.exe" version
```

로컬 인프라를 실행한다.

```powershell
docker compose up -d
```

애플리케이션을 실행한다. (이는 docker compose의 설정에 맞춰서 변경해 주시면 됩니다.)

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DB_URL = "jdbc:mysql://localhost:3307/realtime_chat?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
$env:DB_USERNAME = "realtime_chat"
$env:DB_PASSWORD = "realtime_chat"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:JWT_SECRET = "local-secret-key-must-be-at-least-32-bytes-long"
$env:JWT_ACCESS_TOKEN_VALIDITY_SECONDS = "3600"
$env:JWT_REFRESH_TOKEN_VALIDITY_SECONDS = "1209600"

.\gradlew.bat bootRun
```

다른 PowerShell에서 서버 상태를 확인한다.

```powershell
curl.exe http://localhost:8080/health
```

## 2. 빈 DB로 시작하는 경우

완전히 비어 있는 DB에서 다시 시작해야 할 때만 사용한다. 기존 로컬 데이터가 모두 삭제된다.

```powershell
docker compose down -v
docker compose up -d
```

이후 애플리케이션을 다시 실행하면 Flyway가 기본 스키마를 생성한다.

## 3. 테스트 데이터 생성

아래 스크립트는 다음 데이터를 준비한다.

- 판매자 1명
- 관리자 1명
- 구매자 N명
- 카테고리 2개
- 상품 1개
- 진행 중인 타임세일 1개
- 발급 가능한 쿠폰 정책 1개
- 타임세일용 구매자 토큰 파일
- 쿠폰용 구매자 토큰 파일

`$buyerCount`를 조정하면 요청 수를 늘릴 수 있다. 로컬 smoke 테스트는 10명 정도로 시작하고, 500~1,000건 부하 검증 시에는 500 또는 1,000으로 맞춘다.

```powershell
$hostUrl = "http://localhost:8080"
$password = "plainPassword1"
$buyerCount = 10
$runSuffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$mysqlContainer = "11th-street-mysql-1"

function Invoke-JsonPost($url, $body, $headers = @{}) {
  $json = $body | ConvertTo-Json -Compress
  Invoke-RestMethod -Method Post -Uri $url -ContentType "application/json" -Headers $headers -Body $json
}

# 1. 회원 생성
$sellerEmail = "k6_seller_$runSuffix@example.com"
$adminEmail = "k6_admin_$runSuffix@example.com"

Invoke-JsonPost "$hostUrl/api/v1/members" @{
  email = $sellerEmail
  password = $password
  name = "k6-seller"
}

Invoke-JsonPost "$hostUrl/api/v1/members" @{
  email = $adminEmail
  password = $password
  name = "k6-admin"
}

$buyerEmails = @()
for ($i = 1; $i -le $buyerCount; $i++) {
  $email = "k6_buyer_${runSuffix}_$i@example.com"
  $buyerEmails += $email

  Invoke-JsonPost "$hostUrl/api/v1/members" @{
    email = $email
    password = $password
    name = "k6-buyer-$i"
  }
}

# 2. 역할과 카테고리 준비
# 현재 역할 변경과 카테고리 등록 API가 k6 준비 범위에 없으므로 로컬 DB에 직접 준비한다.
docker exec $mysqlContainer mysql -urealtime_chat -prealtime_chat realtime_chat -e "UPDATE member SET role='SELLER' WHERE email='$sellerEmail';"
docker exec $mysqlContainer mysql -urealtime_chat -prealtime_chat realtime_chat -e "UPDATE member SET role='SUPER_ADMIN' WHERE email='$adminEmail';"
docker exec $mysqlContainer mysql -urealtime_chat -prealtime_chat realtime_chat -e "INSERT INTO category(parent_id, name, depth, sort_order, created_at, updated_at) VALUES (NULL, 'k6-root-$runSuffix', 1, 1, NOW(), NOW());"
docker exec $mysqlContainer mysql -urealtime_chat -prealtime_chat realtime_chat -e "INSERT INTO category(parent_id, name, depth, sort_order, created_at, updated_at) SELECT id, 'k6-child-$runSuffix', 2, 1, NOW(), NOW() FROM category WHERE name='k6-root-$runSuffix' AND depth=1;"

$categoryId = (docker exec $mysqlContainer mysql -N -urealtime_chat -prealtime_chat realtime_chat -e "SELECT id FROM category WHERE name='k6-child-$runSuffix' AND depth=2 LIMIT 1;").Trim()

# 3. 로그인
$sellerLogin = Invoke-JsonPost "$hostUrl/api/v1/auth/login" @{
  email = $sellerEmail
  password = $password
}
$sellerToken = $sellerLogin.data.accessToken

$adminLogin = Invoke-JsonPost "$hostUrl/api/v1/auth/login" @{
  email = $adminEmail
  password = $password
}
$adminToken = $adminLogin.data.accessToken

$buyerTokens = @()
foreach ($email in $buyerEmails) {
  $login = Invoke-JsonPost "$hostUrl/api/v1/auth/login" @{
    email = $email
    password = $password
  }
  $buyerTokens += $login.data.accessToken
}

# 4. 상품 생성
$product = Invoke-JsonPost "$hostUrl/api/v1/products" @{
  name = "k6 load product $runSuffix"
  categoryId = [long]$categoryId
  price = 10000
  stockQuantity = $buyerCount
} @{
  Authorization = "Bearer $sellerToken"
}
$productId = $product.data.id

# 5. 타임세일 생성
$timeSale = Invoke-JsonPost "$hostUrl/api/v1/timesales" @{
  productId = $productId
  salePrice = 9000
  startedAt = "2026-06-01T00:00:00"
  endedAt = "2026-12-31T23:59:59"
  initialQuantity = $buyerCount
} @{
  Authorization = "Bearer $sellerToken"
}
$timeSaleId = $timeSale.data.id

# 6. 쿠폰 정책 생성
$coupon = Invoke-JsonPost "$hostUrl/api/v1/coupons" @{
  name = "k6 load coupon $runSuffix"
  discountType = "FIXED_AMOUNT"
  discountValue = 1000
  issueStartsAt = "2026-06-01T00:00:00"
  issueEndsAt = "2026-12-31T23:59:59"
  totalQuantity = $buyerCount
} @{
  Authorization = "Bearer $adminToken"
}
$couponPolicyId = $coupon.data.id

# 7. 토큰 파일 생성
$timeSaleTokenFile = "$env:TEMP\11th-street-k6-timesale-$runSuffix.json"
$couponTokenFile = "$env:TEMP\11th-street-k6-coupon-$runSuffix.json"

@{ tokens = $buyerTokens } | ConvertTo-Json | Set-Content -LiteralPath $timeSaleTokenFile -Encoding utf8
@{ tokens = $buyerTokens } | ConvertTo-Json | Set-Content -LiteralPath $couponTokenFile -Encoding utf8

[PSCustomObject]@{
  RunSuffix = $runSuffix
  BuyerCount = $buyerCount
  TimeSaleId = $timeSaleId
  CouponPolicyId = $couponPolicyId
  TimeSaleTokenFile = $timeSaleTokenFile
  CouponTokenFile = $couponTokenFile
}
```

마지막에 출력되는 값을 다음 단계의 환경 변수에 사용한다.

## 4. 타임세일 주문 k6 실행

`<TimeSaleTokenFile>`, `<TimeSaleId>`, `<BuyerCount>` 같은 placeholder를 그대로 입력하면 안 된다. 앞 단계에서 저장한 setup JSON을 읽어 환경 변수를 자동으로 채우는 방식을 권장한다.

권장 실행 방식:

```powershell
$setup = Get-Content -LiteralPath "$env:TEMP\11th-street-k6-runbook-last.json" -Raw | ConvertFrom-Json

$env:BASE_URL = "http://localhost:8080"
$env:AUTH_TOKENS_FILE = $setup.TimeSaleTokenFile
$env:TIME_SALE_ID = [string]$setup.TimeSaleId
$env:EXPECTED_STOCK = [string]$setup.BuyerCount
$env:VUS = [string]$setup.BuyerCount
$env:REQUESTS = [string]$setup.BuyerCount
$env:RUN_ID = "empty-db-timesale-001"
$env:RUN_DUPLICATE_PROBE = "false"

& "C:\Program Files\k6\k6.exe" run docs/performance/k6/timesale-order-load.js
```

수동으로 입력할 경우에는 아래 placeholder를 실제 값으로 교체한다.

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:AUTH_TOKENS_FILE = "<TimeSaleTokenFile>"
$env:TIME_SALE_ID = "<TimeSaleId>"
$env:EXPECTED_STOCK = "<BuyerCount>"
$env:VUS = "<BuyerCount>"
$env:REQUESTS = "<BuyerCount>"
$env:RUN_ID = "empty-db-timesale-001"
$env:RUN_DUPLICATE_PROBE = "false"

& "C:\Program Files\k6\k6.exe" run docs/performance/k6/timesale-order-load.js
```

정상 기대 결과:

- `successful_orders`가 `BuyerCount`와 같아야 한다.
- `failed_orders`는 0이어야 한다.
- `timesale_successful_orders <= EXPECTED_STOCK` threshold가 통과해야 한다.
- 결과 파일은 `docs/performance/k6/results/timesale-order-summary.md`에 기록된다.

DB 검증:

```powershell
docker exec 11th-street-mysql-1 mysql -urealtime_chat -prealtime_chat realtime_chat -e "SELECT time_sale_id, status, COUNT(*) AS count FROM time_sale_order WHERE time_sale_id=<TimeSaleId> GROUP BY time_sale_id, status; SELECT time_sale_id, initial_quantity, remaining_quantity FROM time_sale_stock WHERE time_sale_id=<TimeSaleId>;"
```

정상 기대 DB 상태:

- `COMPLETED` 주문 수가 `BuyerCount`와 같아야 한다.
- `remaining_quantity`가 0이어야 한다.

## 5. 쿠폰 발급 k6 실행

타임세일 실행과 같은 구매자 계정을 사용해도 된다. 단, 쿠폰 발급은 쿠폰 정책 단위로 회원당 1회만 성공하므로 같은 쿠폰 정책에 대해 같은 구매자 토큰으로 재실행하면 중복 발급 거절이 발생한다.

`<CouponTokenFile>`, `<CouponPolicyId>`, `<BuyerCount>` 같은 placeholder를 그대로 입력하면 안 된다. 앞 단계에서 저장한 setup JSON을 읽어 환경 변수를 자동으로 채우는 방식을 권장한다.

권장 실행 방식:

```powershell
$setup = Get-Content -LiteralPath "$env:TEMP\11th-street-k6-runbook-last.json" -Raw | ConvertFrom-Json

$env:BASE_URL = "http://localhost:8080"
$env:AUTH_TOKENS_FILE = $setup.CouponTokenFile
$env:COUPON_POLICY_ID = [string]$setup.CouponPolicyId
$env:EXPECTED_COUPON_QUANTITY = [string]$setup.BuyerCount
$env:VUS = [string]$setup.BuyerCount
$env:REQUESTS = [string]$setup.BuyerCount
$env:RUN_ID = "empty-db-coupon-001"
$env:RUN_DUPLICATE_PROBE = "false"

& "C:\Program Files\k6\k6.exe" run docs/performance/k6/coupon-issue-load.js
```

수동으로 입력할 경우에는 아래 placeholder를 실제 값으로 교체한다.

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:AUTH_TOKENS_FILE = "<CouponTokenFile>"
$env:COUPON_POLICY_ID = "<CouponPolicyId>"
$env:EXPECTED_COUPON_QUANTITY = "<BuyerCount>"
$env:VUS = "<BuyerCount>"
$env:REQUESTS = "<BuyerCount>"
$env:RUN_ID = "empty-db-coupon-001"
$env:RUN_DUPLICATE_PROBE = "false"

& "C:\Program Files\k6\k6.exe" run docs/performance/k6/coupon-issue-load.js
```

정상 기대 결과:

- `successful_issues`가 `BuyerCount`와 같아야 한다.
- `failed_issues`는 0이어야 한다.
- `coupon_successful_issues <= EXPECTED_COUPON_QUANTITY` threshold가 통과해야 한다.
- 결과 파일은 `docs/performance/k6/results/coupon-issue-summary.md`에 기록된다.ㅁ

DB 검증:

```powershell
docker exec 11th-street-mysql-1 mysql -urealtime_chat -prealtime_chat realtime_chat -e "SELECT coupon_policy_id, status, COUNT(*) AS count FROM issued_coupon WHERE coupon_policy_id=<CouponPolicyId> GROUP BY coupon_policy_id, status; SELECT id, total_quantity, remaining_quantity FROM coupon_policy WHERE id=<CouponPolicyId>;"
```

정상 기대 DB 상태:

- `ISSUED` 발급 수가 `BuyerCount`와 같아야 한다.
- `remaining_quantity`가 0이어야 한다.

## 6. 결과 해석 기준

타임세일 주문:

- 성공 수가 준비한 구매자 수와 같고 재고가 그만큼 차감되면 정상이다.
- 성공 수가 재고 상한을 초과하면 초과 판매 방지 실패다.
- 실패 수가 발생하면 응답 상태 코드와 DB 주문 저장 여부를 함께 확인한다.

쿠폰 발급:

- 성공 수가 준비한 구매자 수와 같고 잔여 수량이 그만큼 차감되면 정상이다.
- 성공 수가 쿠폰 수량 상한을 초과하면 초과 발급 방지 실패다.
- 같은 쿠폰 정책에 같은 구매자 토큰으로 다시 실행하면 중복 발급 거절이 발생하는 것이 정상이다.

응답 시간:

- 현재 정책 문서에서 허용 응답 시간 기준은 아직 확정되어 있지 않다.
- 따라서 k6 스크립트는 p95 응답 시간을 수집하지만, 응답 시간 threshold로 성공/실패를 판정하지 않는다.

## 7. 반복 실행 시 주의사항

- 같은 타임세일 ID와 같은 구매자 토큰으로 재실행하면 이미 구매한 회원이므로 실패가 발생할 수 있다.
- 같은 쿠폰 정책 ID와 같은 구매자 토큰으로 재실행하면 이미 발급받은 회원이므로 실패가 발생할 수 있다.
- 재실행 시에는 새 구매자를 만들거나, 새 타임세일과 새 쿠폰 정책을 생성한다.
- true 500~1,000건 검증에서는 `$buyerCount`, `initialQuantity`, `totalQuantity`, `VUS`, `REQUESTS`를 같은 값으로 맞추는 것이 가장 단순하다.
