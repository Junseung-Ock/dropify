package com.dropify.order.concurrency;

import com.dropify.order.concurrency.OrderIntegrationTestApplication;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.service.OrderCreationService;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = OrderIntegrationTestApplication.class)
@ActiveProfiles("integration-test")
@Testcontainers
class StockConcurrencyTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("dropify_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OrderCreationService orderCreationService;

    @Autowired
    private NoLockOrderCreationService noLockOrderCreationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        Product product = Product.builder()
                .name("테스트 상품")
                .price(10000L)
                .stockQuantity(1)
                .build();
        productRepository.save(product);
        productId = product.getId();
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("분산 락 적용 시 100명 동시 요청에서 재고 1개에 정확히 1건만 주문이 생성된다")
    void concurrentOrders_withDistributedLock_onlyOneSucceeds() throws InterruptedException {
        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1L;
            new Thread(() -> {
                try {
                    startLatch.await();
                    PlaceOrderRequest request = mock(PlaceOrderRequest.class);
                    when(request.getProductId()).thenReturn(productId);
                    when(request.getQuantity()).thenReturn(1);
                    orderCreationService.create(userId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);
        assertThat(orderRepository.count()).isEqualTo(1);

        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("분산 락 미적용 시 100명 동시 요청에서 1건 초과 주문이 생성된다 (데이터 오염)")
    void concurrentOrders_withoutDistributedLock_causesDataCorruption() throws InterruptedException {
        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1L;
            new Thread(() -> {
                try {
                    startLatch.await();
                    PlaceOrderRequest request = mock(PlaceOrderRequest.class);
                    when(request.getProductId()).thenReturn(productId);
                    when(request.getQuantity()).thenReturn(1);
                    noLockOrderCreationService.create(userId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        // 락 없이는 여러 스레드가 동시에 재고 검증을 통과해 1건 초과 주문이 생성된다
        assertThat(orderRepository.count()).isGreaterThan(1);
    }
}
