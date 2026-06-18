# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all modules
./gradlew build

# Run the application (app module is the entry point)
./gradlew :app:bootRun

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :product:test

# Run a single test class
./gradlew :product:test --tests "com.dropify.product.service.ProductServiceTest"

# Run a single test method
./gradlew :product:test --tests "com.dropify.product.service.ProductServiceTest.delete_productNotFound_throwsBusinessException"
```

## Local Development Setup

Start infrastructure before running the app:
```bash
docker compose up -d mysql redis kafka zookeeper
```

Required environment variables:
```
Local_DB_URL=jdbc:mysql://localhost:3306/<db>
DB_USERNAME=<user>
DB_PASSWORD=<password>
Local_REDIS_HOST=localhost
REDIS_PORT=6379
Local_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
JWT_SECRET=<min-32-char-secret>
```

Active profile defaults to `local`. Flyway runs migrations automatically on startup (`app/src/main/resources/db/migration/`).

Swagger UI: `http://localhost:8080/swagger-ui.html` (no auth required)

## Architecture

### Multi-module Gradle layout

```
dropify/
├── app/          # Entry point — Spring Boot main class, SecurityConfig, assembles all modules
├── common/       # Shared: BaseEntity, ApiResponse, BusinessException, ErrorCode, GlobalExceptionHandler
├── user/         # Auth domain — signup/login/logout/reissue, JWT, Spring Security beans
├── product/      # Product domain — CRUD, QueryDSL search, Redis caching, 재고 관리
├── order/        # Order domain (entities defined, service layer pending)
├── payment/      # Payment domain (entities defined, service layer pending)
└── notification/ # Notification domain (entities defined, service layer pending)
```

Each domain module depends on `:common`. The `app` module depends on all domain modules and owns `SecurityConfig` — this centralizes Spring Security wiring while domain modules provide their beans (filters, handlers, providers).

### Security

Stateless JWT authentication. `JwtAuthenticationFilter` (in `user`) is registered in `SecurityConfig` (in `app`).

Public routes: `POST /api/auth/**`, `GET /api/products/**`, Swagger endpoints.

Admin routes: `POST /api/admin/**` — `@PreAuthorize("hasRole('ADMIN')")` 로 메서드 레벨에서 제어.

Refresh tokens are stored in Redis as `@RedisHash("refreshToken")` keyed by `userId` (TTL 7 days). On reissue, the token is rotated — the old refresh token is invalidated immediately.

### Response & Error conventions

All controllers return `ApiResponse<T>`:
- Success: `ApiResponse.ok(data)` or `ApiResponse.ok()`
- Error: `ApiResponse.error(code, message)`

Errors flow through `BusinessException` → `ErrorCode` enum → `GlobalExceptionHandler`. When adding a new error, add it to `ErrorCode` first, then throw `new BusinessException(ErrorCode.XXX)`.

### Data access patterns

- JPA entities extend `BaseEntity` (`createdAt`, `updatedAt` via JPA Auditing)
- Entities use `@NoArgsConstructor(access = PROTECTED)` and package-private `@Builder` constructors; state changes go through domain methods (e.g. `product.update(...)`, `order.cancel()`)
- Complex queries use QueryDSL via `XxxRepositoryCustom` / `XxxRepositoryCustomImpl` pattern
- MapStruct mappers (`@Mapper(componentModel = "spring")`) convert entities to DTOs

### Redis 재고 관리

재고는 DB와 Redis 두 곳에서 관리됩니다.

- 키 형식: `stock:{productId}`
- **앱 시작 시** `StockSyncRunner`가 DB의 모든 상품 재고를 Redis에 초기화 (키가 없을 때만, 재시작 시 중복 방지)
- **재고 변경 시** DB를 먼저 수정하고, 트랜잭션 커밋 후 Redis를 동기화 (`TransactionSynchronizationManager.registerSynchronization`)
- `StockService.decreaseStock()` — 주문 모듈에서 호출, 재고 차감
- `StockService.replenishStock()` — `POST /api/admin/products/{productId}/stock` (ADMIN 전용), 재고 보충

### 재고 이력 (`stock_histories`)

재고가 변경될 때마다 `StockChangeAspect`가 AOP로 자동 기록합니다.

- `@StockChange(type = StockChangeType.XXX)` 어노테이션을 메서드에 붙이면 적용됨
- 메서드 실행 전후 재고를 DB에서 조회해 `beforeQuantity` / `afterQuantity` 기록
- `changedBy`: 인증된 사용자면 userId, 미인증이면 `"SYSTEM"`
- `changeType`: `SYNC` (앱 시작), `DECREASE` (주문), `REPLENISH` (보충)
- `@StockChange` 메서드의 첫 번째 파라미터는 반드시 `Long productId` 여야 함

### Caching

`product` 모듈이 단건 조회를 Redis에 캐싱합니다. `@Cacheable(value = "product", key = "#id")` (TTL 10분), 수정/삭제 시 `@CacheEvict`. `CacheConfig`에서 Jackson 폴리모픽 타입 정보를 활성화 — 역직렬화 시 필수.

### Testing patterns

| Layer | Annotation | Notes |
|---|---|---|
| Service | `@ExtendWith(MockitoExtension.class)` | Pure unit, Mockito mocks |
| Controller | `@WebMvcTest` + `excludeAutoConfiguration = SecurityAutoConfiguration.class` | MockMvc, Security disabled |
| Repository | `@DataJpaTest` + `@Import({JpaConfig.class, QueryDslConfig.class})` | H2 in-memory |

Repository 테스트는 `JpaConfig`와 `QueryDslConfig`를 명시적으로 `@Import` 해야 함 — `@DataJpaTest`가 자동 설정하지 않음.
