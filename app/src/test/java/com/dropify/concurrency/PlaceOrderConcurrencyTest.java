package com.dropify.concurrency;

import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.service.OrderCreationService;
import com.dropify.payment.service.PaymentService;
import com.dropify.web.usecase.PlaceOrderUseCaseImpl;
import com.dropify.payment.domain.repository.PaymentRepository;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.domain.repository.StockHistoryRepository;
import com.dropify.product.service.ProductService;
import com.dropify.product.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {AppIntegrationTestApplication.class, PlaceOrderConcurrencyTest.TestConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-test")
@Testcontainers
class PlaceOrderConcurrencyTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        NoLockPlaceOrderProcessor noLockPlaceOrderProcessor(ProductService productService,
                OrderCreationService orderCreationService,
                StockService stockService,
                PaymentService paymentService) {
            return new NoLockPlaceOrderProcessor(productService, orderCreationService, stockService, paymentService);
        }
    }

    static class NoLockPlaceOrderProcessor {
        private final ProductService productService;
        private final OrderCreationService orderCreationService;
        private final StockService stockService;
        private final PaymentService paymentService;

        NoLockPlaceOrderProcessor(ProductService productService,
                                   OrderCreationService orderCreationService,
                                   StockService stockService,
                                   PaymentService paymentService) {
            this.productService = productService;
            this.orderCreationService = orderCreationService;
            this.stockService = stockService;
            this.paymentService = paymentService;
        }

        @Transactional
        public PlaceOrderResponse process(Long userId, PlaceOrderRequest request) {
            Product product = productService.getEntityById(request.getProductId());
            PlaceOrderResponse response = orderCreationService.create(
                    userId, product.getId(), request.getQuantity(), product.getPrice());
            stockService.decreaseStock(product.getId(), request.getQuantity());
            paymentService.createPendingPayment(response.getOrderId(), response.getTotalAmount());
            return response;
        }
    }

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

    @Autowired private PlaceOrderUseCaseImpl placeOrderUseCaseImpl;
    @Autowired private NoLockPlaceOrderProcessor noLockPlaceOrderProcessor;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private StockHistoryRepository stockHistoryRepository;

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
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        stockHistoryRepository.deleteAll();
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
            final String idempotencyKey = "key-" + userId;
            new Thread(() -> {
                try {
                    startLatch.await();
                    PlaceOrderRequest request = mock(PlaceOrderRequest.class);
                    when(request.getProductId()).thenReturn(productId);
                    when(request.getQuantity()).thenReturn(1);
                    placeOrderUseCaseImpl.execute(userId, request, idempotencyKey);
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

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1L;
            new Thread(() -> {
                try {
                    startLatch.await();
                    PlaceOrderRequest request = mock(PlaceOrderRequest.class);
                    when(request.getProductId()).thenReturn(productId);
                    when(request.getQuantity()).thenReturn(1);
                    noLockPlaceOrderProcessor.process(userId, request);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertThat(orderRepository.count()).isGreaterThan(1);
    }
}
