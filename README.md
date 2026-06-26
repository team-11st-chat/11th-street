# 11th-street

3인 팀 커머스 MVP 프로젝트입니다. 현재 저장소는 Spring Boot 기반 API 서버, MySQL, Redis 로컬 실행 환경과 Gradle 검증 태스크를 제공합니다.

## 실행 조건

- Java 21
- Docker Desktop 또는 Docker Compose
- 로컬 기본 포트
  - API 서버: `8080`
  - MySQL: `3307`
  - Redis: `6379`
- Spring profile 기본값: `local`
- 로컬 DB는 `docker-compose.yml`의 MySQL 서비스 값을 사용합니다.

## 로컬 실행

1. MySQL과 Redis를 실행합니다.

```powershell
docker compose up -d
```

2. 애플리케이션 실행에 필요한 환경변수를 설정합니다.

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
```

3. 서버를 실행합니다.

```powershell
.\gradlew.bat bootRun
```

4. Health Check를 확인합니다.

```powershell
curl.exe http://localhost:8080/health
```

5. 실행을 종료할 때는 로컬 인프라를 내립니다.

```powershell
docker compose down
```

데이터까지 초기화해야 하는 경우에만 다음 명령을 사용합니다.

```powershell
docker compose down -v
```

## 테스트 실행

일반 테스트는 `integration` 태그를 제외하고 실행됩니다.

```powershell
.\gradlew.bat test
```

통합 테스트는 `test` profile과 `integration` 태그를 사용합니다. Redis 연동 테스트가 포함되어 있으므로 먼저 로컬 Redis를 실행합니다.

```powershell
docker compose up -d redis
.\gradlew.bat integrationTest
```

전체 검증은 일반 테스트와 통합 테스트를 함께 실행합니다.

```powershell
docker compose up -d redis
.\gradlew.bat check
```

검색 성능 리포트 테스트는 기본 실행에서 제외됩니다. 필요할 때만 명시적으로 활성화합니다.

```powershell
$env:PERFORMANCE_TEST = "true"
.\gradlew.bat integrationTest
```

## 필수 기능 시연

필수 기능 시연 순서는 [docs/demo-script.md](docs/demo-script.md)를 따릅니다.

현재 시연 범위는 MVP Wiki의 최종 시연 시나리오 중 로컬에서 확인 가능한 회원, 상품, 검색, 인기 검색어, 타임세일 주문, 쿠폰 발급 흐름입니다. Redis Lock 기반 실제 분산 락과 멱등성 구현은 아직 `FakeLockManager`, `FakeIdempotencyManager`를 사용하므로, 동시성 Before/After 최종 비교는 실제 구현체 교체 후 다시 검증해야 합니다.
