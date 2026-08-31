<div align="center">

# 🛍️ Dropify

**한정판 굿즈 선착순 구매 플랫폼**

Java 21 · Spring Boot 3.3 · Redis 분산 락 · Testcontainers 동시성 검증

</div>

---

## 📌 프로젝트 개요

한정판 굿즈의 선착순 구매에서 발생하는 **동시성 문제**를 해결하는 것을 목표로 설계한 커머스 플랫폼입니다.

짧은 시간에 요청이 몰리고, 재고를 초과해 판매되면 안 되며, 같은 요청이 두 번 들어와도 주문이 하나만 생성되어야 하는 도메인을 의도적으로 선택했습니다. 기능 개수를 늘리기보다 **하나의 문제를 끝까지 파고들어 검증하는 것**에 무게를 두고 진행하고 있습니다.

---

## 🚦 진행 현황

전체 6단계 로드맵 중 **3단계까지 완료**했습니다.

| 단계 | 내용 | 상태 |
|------|------|------|
| 1 | 환경 세팅 (멀티모듈 · Docker · Flyway) | ✅ 완료 |
| 2 | 회원 · 상품 · 재고 | ✅ 완료 |
| 3 | 주문 · 동시성 제어 | ✅ 완료 |
| 4 | 결제 연동 (Toss) · Kafka Consumer | 🚧 진행 예정 |
| 5 | 모니터링 · 알림 | 🚧 진행 예정 |
| 6 | 마무리 · 배포 | 🚧 진행 예정 |

> 동시성 제어를 가장 먼저 검증하고 그 위에 결제·비동기 처리를 얹는 순서로 설계했습니다.
> 결제 흐름은 주문의 정합성이 보장된 뒤에야 의미가 있다고 판단했기 때문입니다.
>
> `payment` · `notification` 모듈은 현재 도메인 엔티티와 DB 스키마만 정의된 상태이며,
> 서비스 로직과 Kafka Consumer는 4단계에서 구현할 예정입니다.

---

## 🛠 기술 스택

| 분류 | 기술 | 상태 |
|------|------|------|
| Language | Java 21 | ✅ |
| Framework | Spring Boot 3.3, Spring Security, Spring Data JPA | ✅ |
| Database | MySQL 8.0, Redis 7 | ✅ |
| Query | JPA, QueryDSL | ✅ |
| 분산 락 | Redisson | ✅ |
| 테스트 | JUnit 5, Testcontainers, AssertJ, Mockito | ✅ |
| DB 마이그레이션 | Flyway | ✅ |
| 문서화 | SpringDoc OpenAPI 3 (Swagger UI) | ✅ |
| 인프라 | Docker Compose | ✅ |
| Message Queue | Apache Kafka (Producer 구현, Consumer 예정) | 🚧 |
| 결제 | Toss Payments API | 🚧 예정 |
| 모니터링 | Prometheus · Grafana · AlertManager (설정 작성, 연동 예정) | 🚧 |

---

## 🏗 시스템 아키텍처

```
[Client]
    │
    ▼
[Spring Boot 3 — 모놀리식 멀티모듈]
    ├── app          실행 진입점 · 보안 설정 · Flyway 마이그레이션
    ├── common       공통 예외 · 응답 포맷 · BaseEntity
    ├── user         회원가입 · 로그인 · JWT 인증
    ├── product      상품 CRUD · 재고 · 재고 변경 이력
    ├── order        주문 생성 · 분산 락 · 멱등성 · Kafka Producer
    ├── payment      (엔티티 정의 완료 · 로직 구현 예정)
    └── notification (엔티티 정의 완료 · 로직 구현 예정)
          │
          ├── MySQL 8   주문 · 상품 · 회원 데이터
          ├── Redis 7   재고 사전 확인 · 분산 락 · 멱등성 키
          └── Kafka     payment-request 이벤트 발행
```

**모놀리식 멀티모듈을 선택한 이유**

단일 JAR로 배포하는 단순함은 유지하면서 도메인 경계는 모듈 단위로 강제하고 싶었습니다.
모듈 간 의존을 `build.gradle`에서 명시적으로 선언하게 되므로, 어떤 도메인이 어떤 도메인을 참조하는지가 코드가 아니라 빌드 설정에 드러납니다. 이후 특정 도메인만 분리해야 할 때 경계를 다시 긋는 비용을 줄이려는 의도입니다.

---

## ⚙️ 핵심 구현

### 1. 선착순 동시성 제어

주문 요청은 다음 순서로 처리됩니다.

```
① 멱등성 키 확인      이미 처리된 요청이면 저장된 응답을 그대로 반환
② Redis 재고 사전 확인  재고가 명백히 없는 요청을 락 앞에서 차단
③ 분산 락 획득        상품 단위 (order:lock:{productId})
④ DB 재고 최종 검증 · 차감 · 주문 생성 (PENDING)
⑤ 락 해제
⑥ payment-request 이벤트 발행
```

**설계 판단**

**Redis는 1차 필터, DB가 최종 판단자로 두었습니다.**
Redis 재고 값만으로 판단하면 캐시와 DB가 어긋났을 때 재고를 초과해 판매할 수 있습니다.
그래서 Redis 확인은 "명백히 재고가 없는 요청을 걸러내는" 용도로만 쓰고, 실제 차감 가능 여부는 락 안에서 DB로 다시 확인합니다. Redis 재고 동기화도 `afterCommit` 시점에 수행해, 트랜잭션이 롤백되면 캐시가 먼저 줄어드는 상황을 방지했습니다.

**락 Aspect를 가장 바깥 레이어에 두었습니다.**
`DistributedLockAspect`에 `@Order(-1)`을 지정해 재고 이력 기록 Aspect(`@Order(0)`)와 트랜잭션 어드바이저보다 먼저 실행되도록 했습니다. 락이 트랜잭션 안쪽에 걸리면 커밋 전에 락이 풀려 다른 요청이 커밋 이전 상태를 읽을 수 있기 때문입니다.

**leaseTime을 명시해 Watchdog을 비활성화했습니다.**
Redisson은 leaseTime을 지정하지 않으면 Watchdog이 락을 자동 연장합니다. 서버가 비정상 종료되면 락이 계속 살아남아 해당 상품의 주문이 막힐 수 있어, 5초를 명시해 반드시 만료되도록 했습니다. 대신 leaseTime 초과로 락이 자동 해제된 뒤 다른 스레드가 잡은 락을 잘못 해제하지 않도록, 해제 시 `isHeldByCurrentThread()`로 소유권을 확인합니다.

```java
@DistributedLock(key = "'order:lock:' + #request.productId")
@Transactional
public PlaceOrderResponse create(Long userId, PlaceOrderRequest request) {
    Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

    Order order = Order.builder()
            .userId(userId)
            .totalAmount(product.getPrice() * request.getQuantity())
            .build();
    order.addOrderItem(OrderItem.builder()
            .order(order)
            .productId(product.getId())
            .quantity(request.getQuantity())
            .unitPrice(product.getPrice())
            .build());
    orderRepository.save(order);

    // 재고 부족 시 INSUFFICIENT_STOCK 예외 → 트랜잭션 롤백
    stockService.decreaseStock(product.getId(), request.getQuantity());

    return new PlaceOrderResponse(order);
}
```

락 키는 SpEL로 선언해 메서드 파라미터에서 동적으로 생성합니다. 상품 단위로 락을 나눈 이유는 서로 다른 상품의 주문끼리는 경합할 이유가 없기 때문입니다.

---

### 2. 동시성 검증 — 락 유무 비교

락을 걸었다는 사실보다 **락이 없으면 실제로 깨진다는 것**을 함께 보여주는 편이 검증으로서 의미가 있다고 판단했습니다. 그래서 동일한 로직에서 락만 제거한 `NoLockOrderCreationService`를 테스트 전용으로 두고 두 경우를 비교했습니다.

검증 환경은 Testcontainers로 MySQL 8과 Redis 7을 실제로 띄워 구성했습니다. 임베디드 DB로는 실제 트랜잭션 격리 수준에서의 경합을 재현할 수 없다고 보았기 때문입니다.

```
@Test
@DisplayName("분산 락 적용 시 100명 동시 요청에서 재고 1개에 정확히 1건만 주문이 생성된다")
void concurrentOrders_withDistributedLock_onlyOneSucceeds() throws InterruptedException {
    // CountDownLatch로 100개 스레드를 동시에 출발시켜 경합 상황을 재현
    ...
    assertThat(successCount.get()).isEqualTo(1);
}
```

| 조건 | 재고 | 동시 요청 | 성공 주문 |
|------|------|-----------|-----------|
| 분산 락 적용 | 1 | 100 | 1 |
| 분산 락 미적용 | 1 | 100 | 1건 초과 (재고 초과 판매 발생) |

---

### 3. 멱등성 처리

네트워크 오류나 사용자의 재시도로 같은 주문 요청이 두 번 들어오는 경우를 방지하기 위해, 클라이언트가 전달한 멱등성 키를 Redis에 24시간 보관하고 동일 키로 재요청이 오면 저장된 응답을 그대로 반환합니다.

키는 `idempotency:{userId}:{key}` 형태로 사용자 단위 스코프를 두어, 다른 사용자가 우연히 같은 키를 보내더라도 서로 간섭하지 않도록 했습니다.

응답 캐시는 이벤트 발행보다 **먼저** 저장합니다. 이벤트 발행이 실패해 클라이언트가 재시도하더라도 주문이 다시 생성되지 않도록 하기 위함입니다.

---

### 4. 재고 변경 이력

재고가 변동되는 모든 지점에 이력을 남기기 위해 `@StockChange` 어노테이션과 AOP를 사용했습니다. 서비스 로직마다 이력 저장 코드를 중복해서 넣으면 새 경로가 추가될 때 누락되기 쉽다고 판단해, 어노테이션 선언만으로 이력이 남도록 분리했습니다.

---

## 📁 프로젝트 구조

```
dropify/
├── app/            실행 진입점, SecurityConfig, Flyway 마이그레이션
├── common/         BusinessException, ErrorCode, ApiResponse, BaseEntity
├── user/           User, RefreshToken, JWT 인증 필터 · 토큰 프로바이더
├── product/        Product, StockHistory, QueryDSL 검색, 재고 서비스
├── order/          Order, OrderItem, 분산 락(AOP), 멱등성, Kafka Producer
├── payment/        Payment 엔티티 (로직 구현 예정)
├── notification/   Notification 엔티티 (로직 구현 예정)
└── docker/         MySQL · Prometheus 설정
```

---

## 📊 DB 스키마

Flyway로 관리하며 현재 V4까지 적용되어 있습니다.

| 테이블 | 설명 |
|--------|------|
| `users` | 회원 정보 · 권한 |
| `products` | 상품 · 재고 수량 |
| `stock_histories` | 재고 변경 이력 (증감 사유 포함) |
| `orders` | 주문 (PENDING / COMPLETED / CANCELLED) |
| `order_items` | 주문 상품 항목 |
| `payments` | 결제 정보 (스키마 정의 완료, 연동 예정) |
| `notifications` | 알림 (스키마 정의 완료, 연동 예정) |

| 마이그레이션 | 내용 |
|--------------|------|
| `V1__init` | 초기 스키마 |
| `V2__add_role_to_users` | 회원 권한 컬럼 추가 |
| `V3__add_stock_histories` | 재고 이력 테이블 추가 |
| `V4__optimize_order_indexes` | 주문 조회 인덱스 최적화 |

---

## 🧪 테스트

| 구분 | 내용 |
|------|------|
| 단위 테스트 | 인증, JWT, 주문, 멱등성, 재고, 상품 서비스 |
| 컨트롤러 테스트 | 인증 · 주문 · 상품 · 관리자 재고 API |
| 통합 테스트 | Testcontainers 기반 동시성 검증 (MySQL · Redis 실제 구동) |

```bash
./gradlew test
```

---

## 🚀 로컬 실행

**사전 요구사항** — Docker, JDK 21

```bash
# 1. 클론
git clone https://github.com/Junseung-Ock/dropify.git
cd dropify

# 2. 환경 변수 설정
cp .env.example .env

# 3. 인프라 실행 (MySQL · Redis · Kafka)
docker compose up -d

# 4. 애플리케이션 실행
./gradlew :app:bootRun

# 5. API 문서
# http://localhost:8080/swagger-ui/index.html
```

---

## 🔜 다음 단계

- **결제 연동** — Toss Payments 승인 API 연동 및 `payment-request` Consumer 구현
- **보상 트랜잭션** — 결제 실패 시 재고 복구 및 주문 취소 처리
- **알림** — 결제 완료 이벤트 기반 알림 발송
- **모니터링** — Micrometer 커스텀 메트릭(주문 처리량, 락 획득 실패율) 수집 및 알림 연동
- **배포** — CI 파이프라인 구성 및 운영 환경 배포