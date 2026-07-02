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

## 프론트엔드 로컬 실행

커머스 프론트엔드는 `frontend/` 디렉터리에 있으며 Vite로 실행합니다.
먼저 Spring Boot API 서버를 실행한 뒤 프론트엔드 개발 서버를 시작합니다.

1. 프론트엔드 의존성을 설치합니다.

```powershell
cd frontend
npm install
```

2. 백엔드 API URL을 설정합니다.

```powershell
Copy-Item .env.example .env
```

기본 API 주소는 `http://localhost:8080`입니다.
백엔드를 다른 주소에서 실행하는 경우 `frontend/.env`의 `VITE_API_BASE_URL` 값을 변경합니다.

3. 프론트엔드 개발 서버를 실행합니다.

```powershell
npm run dev
```

4. 브라우저에서 앱을 엽니다.

```text
http://localhost:5350
```

프론트엔드에서 확인할 수 있는 주요 흐름은 다음과 같습니다.

- 실제 상품 API 기반 상품 목록/검색
- 상품 검색 후 인기 검색어 갱신
- 타임세일 목록 조회와 주문 요청
- 쿠폰 정책 선택과 쿠폰 발급 요청
- 상품/CS 문의방과 STOMP 실시간 채팅
- SELLER, SUPER_ADMIN 백오피스 진입점

프론트엔드 변경을 커밋하기 전에는 다음 검증을 실행합니다.

```powershell
cd frontend
npm run build
```

`node_modules`, `dist`, `.vite`, `*.tsbuildinfo` 같은 생성물은 커밋하지 않습니다.

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

## 문서 위치

- `wiki/`: 요구사항, 정책, API 명세, ERD, 성능 분석처럼 장기적으로 추적할 Wiki 문서를 관리합니다.
- `docs/`: 로컬 실행 절차, 이슈별 작업 기록, 부하 테스트 스크립트와 결과처럼 저장소 안에서 함께 검증해야 하는 문서를 관리합니다.
- 요구사항, 도메인, 명세 성격의 장기 산출물은 별도 `docs/` 트리로 복제하지 않고 `wiki/` 작업트리에서 편집합니다.

## DB 마이그레이션

Flyway 마이그레이션은 `src/main/resources/db/migration` 아래에서 관리합니다.
현재 초기 배포 전 단계이므로 이전 이력들은 모두 `V1__init_schema.sql`로 스쿼시(Squash) 통합되어 하나의 파일로 관리됩니다.

로컬 DB를 완전히 초기화해야 하는 경우에만 볼륨을 삭제한 뒤 다시 기동합니다.

```powershell
docker compose down -v
docker compose up -d mysql redis
```

## CI/CD 배포

PR 검증은 `.github/workflows/pr-verify.yml`, 빌드와 배포 파이프라인은 `.github/workflows/ci-cd.yml`에서 관리합니다.
`main`, `develop` 브랜치에 push되거나 수동 실행할 때 다음 순서로 동작합니다.

1. Gradle compile, unit test, integration test를 실행합니다.
2. 테스트가 성공하면 Docker 이미지 빌드 가능 여부를 확인합니다.
3. PR에서는 ECR, Launch Template, Auto Scaling Group, Parameter Store 접근을 dry run으로 검증합니다.
4. push 또는 수동 실행에서는 이미지를 Amazon ECR에 commit SHA 태그와 `latest` 태그로 push합니다.
5. `main` 브랜치 배포 또는 수동 실행 시 Launch Template 새 버전을 생성합니다.
6. Auto Scaling Group Instance Refresh로 신규 EC2 인스턴스를 교체합니다.
7. EC2 User Data가 Redis를 인스턴스 서비스로 실행하고, Parameter Store 값을 `.env.runtime` 파일로 만든 뒤 애플리케이션 컨테이너를 실행합니다.
8. 배포 후 Health Check를 최대 20회 호출하고, 모두 실패하면 이전 정상 버전으로 롤백합니다.

### Health Check와 자동 롤백

EC2 User Data는 새 컨테이너를 실행한 뒤 `http://localhost:${SERVER_PORT:-8080}/health`를 최대 20회 확인합니다.
모두 실패하면 User Data가 실패로 종료되고, 새 인스턴스는 Instance Refresh의 정상 교체 대상으로 인정되지 않습니다.

GitHub Actions는 배포 전에 Launch Template의 기존 default version을 저장합니다.
Instance Refresh 또는 외부 `HEALTHCHECK_URL` 검증이 실패하면 Launch Template default version을 이전 값으로 되돌린 뒤 Instance Refresh를 다시 시작합니다.
이 흐름은 실패한 새 배포 버전이 Auto Scaling Group의 기본 시작 구성으로 남지 않게 하기 위한 복구 절차입니다.

Auto Scaling Group 헬스체크는 애플리케이션 장애를 감지할 수 있도록 ELB Target Group 헬스체크와 연동하는 것을 권장합니다.
EC2 헬스체크만 사용하면 인스턴스 부팅은 성공했지만 애플리케이션 컨테이너가 비정상인 상태를 Instance Refresh가 성공으로 판단할 수 있습니다.
User Data 성공 여부를 인프라 수준에서 명시적으로 제어해야 하는 경우에는 Auto Scaling Lifecycle Hook으로 부팅 및 애플리케이션 준비 완료 신호를 전달합니다.

수동 복구가 필요한 경우에는 AWS 콘솔 또는 CLI에서 Launch Template default version을 마지막 정상 버전으로 변경한 뒤 Auto Scaling Group Instance Refresh를 다시 실행합니다.

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
배포 환경의 MySQL은 RDS를 사용하고, Redis는 EC2 인스턴스에 설치된 로컬 서비스를 사용합니다.
따라서 `DB_URL`은 RDS endpoint를, `REDIS_HOST`는 `localhost`를 사용합니다.

```text
/11th-street/prod/SPRING_PROFILES_ACTIVE
/11th-street/prod/SERVER_PORT
/11th-street/prod/DB_URL
/11th-street/prod/DB_USERNAME
/11th-street/prod/DB_PASSWORD
/11th-street/prod/REDIS_HOST
/11th-street/prod/REDIS_PORT
/11th-street/prod/JWT_SECRET
/11th-street/prod/JWT_ACCESS_TOKEN_VALIDITY_SECONDS
/11th-street/prod/JWT_REFRESH_TOKEN_VALIDITY_SECONDS
```

## 필수 기능 시연

필수 기능 시연 순서는 [docs/demo-script.md](docs/demo-script.md)를 따릅니다.

현재 시연 범위는 MVP Wiki의 최종 시연 시나리오 중 로컬에서 확인 가능한 회원, 상품, 검색, 인기 검색어, 타임세일 주문, 쿠폰 발급 흐름입니다. 로컬 실행 profile에서는 Redis 기반 `RedissonLockManager`가 분산 락을, `RedisIdempotencyManager`가 Request-ID 멱등성을 처리합니다. 둘 다 단일 구현체를 모든 profile에서 사용하며(동시성 테스트는 Mockito로 `LockManager`·`IdempotencyManager`를 대체), 분산 락은 부하 측정 전용 `nolock` profile에서만 `NoOpLockManager`로 대체됩니다.
