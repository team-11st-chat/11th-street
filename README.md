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

## Docker Compose 로컬 실행

1. Application JAR를 빌드합니다.

```powershell
.\gradlew.bat bootJar
```

2. Application, MySQL, Redis를 함께 빌드하고 실행합니다.

```powershell
docker compose up --build
```

백그라운드에서 실행하려면 다음 명령을 사용합니다.

```powershell
docker compose up --build -d
```

3. Health Check를 확인합니다.

```powershell
curl.exe http://localhost:8080/health
```

4. 실행을 종료할 때는 컨테이너를 내립니다.

```powershell
docker compose down
```

데이터까지 초기화해야 하는 경우에만 다음 명령을 사용합니다.

```powershell
docker compose down -v
```

## Gradle 로컬 실행

1. MySQL과 Redis를 실행합니다.

```powershell
docker compose up -d mysql redis
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

## CI/CD 배포

배포 파이프라인은 `.github/workflows/ci-cd.yml`에서 관리합니다.
`main`, `develop` 브랜치에 push되거나 수동 실행할 때 다음 순서로 동작합니다.

1. Gradle compile, unit test, integration test를 실행합니다.
2. 테스트가 성공하면 Docker 이미지 빌드 가능 여부를 확인합니다.
3. PR에서는 ECR, Launch Template, Auto Scaling Group, Parameter Store 접근을 dry run으로 검증합니다.
4. push 또는 수동 실행에서는 이미지를 Amazon ECR에 commit SHA 태그와 `latest` 태그로 push합니다.
5. `main` 브랜치 배포 또는 수동 실행 시 Launch Template 새 버전을 생성합니다.
6. Auto Scaling Group Instance Refresh로 신규 EC2 인스턴스를 교체합니다.
7. EC2 User Data가 Parameter Store 값을 `.env.runtime` 파일로 만든 뒤 Docker Compose로 Application, MySQL, Redis 컨테이너를 실행합니다.

GitHub Actions에는 다음 값을 등록해야 합니다.

| 구분 | 이름 | 설명 |
| --- | --- | --- |
| Secret | `AWS_ROLE_ARN` | GitHub OIDC가 AssumeRole 할 IAM Role ARN |
| Variable | `AWS_ACCOUNT_ID` | AWS 계정 ID. 예: `388784542084` |
| Variable | `AWS_REGION` | AWS Region. 예: `ap-northeast-2` |
| Variable | `ECR_REPOSITORY` | ECR Repository 이름. 예: `11th-street-app` |
| Variable | `LAUNCH_TEMPLATE_ID` | 배포 대상 Auto Scaling Group이 사용하는 Launch Template ID |
| Variable | `ASG_NAME` | Instance Refresh를 실행할 Auto Scaling Group 이름 |
| Variable | `HEALTHCHECK_URL` | 배포 후 확인할 Health Check URL. 예: `https://example.com/health` |

EC2 Instance Role에는 ECR 이미지 pull 권한과 Parameter Store 읽기 권한이 필요합니다.
런타임 환경변수는 Parameter Store의 `/11th-street/prod` 경로 아래에 저장합니다.
`DB_PASSWORD`, `JWT_SECRET`처럼 민감한 값은 `SecureString`으로 저장합니다.
배포 환경의 MySQL과 Redis는 EC2 내부 Docker Compose 서비스로 실행되므로 `DB_URL`의 host는 `mysql`, `REDIS_HOST`는 `redis`로 설정합니다.

```text
/11th-street/prod/SPRING_PROFILES_ACTIVE
/11th-street/prod/SERVER_PORT
/11th-street/prod/DB_URL
/11th-street/prod/DB_USERNAME
/11th-street/prod/DB_PASSWORD
/11th-street/prod/MYSQL_DATABASE
/11th-street/prod/MYSQL_USER
/11th-street/prod/MYSQL_PASSWORD
/11th-street/prod/MYSQL_ROOT_PASSWORD
/11th-street/prod/REDIS_HOST
/11th-street/prod/REDIS_PORT
/11th-street/prod/JWT_SECRET
/11th-street/prod/JWT_ACCESS_TOKEN_VALIDITY_SECONDS
/11th-street/prod/JWT_REFRESH_TOKEN_VALIDITY_SECONDS
```

## 필수 기능 시연

필수 기능 시연 순서는 [docs/demo-script.md](docs/demo-script.md)를 따릅니다.

현재 시연 범위는 MVP Wiki의 최종 시연 시나리오 중 로컬에서 확인 가능한 회원, 상품, 검색, 인기 검색어, 타임세일 주문, 쿠폰 발급 흐름입니다. 로컬 실행 profile에서는 Redis 기반 `RedissonLockManager`가 분산 락을, `RedisIdempotencyManager`가 Request-ID 멱등성을 처리합니다. 둘 다 단일 구현체를 모든 profile에서 사용하며(동시성 테스트는 Mockito로 `LockManager`·`IdempotencyManager`를 대체), 분산 락은 부하 측정 전용 `nolock` profile에서만 `NoOpLockManager`로 대체됩니다.
