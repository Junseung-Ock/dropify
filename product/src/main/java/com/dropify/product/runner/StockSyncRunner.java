package com.dropify.product.runner;

import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.entity.StockChangeType;
import com.dropify.product.domain.entity.StockHistory;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.domain.repository.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncRunner implements ApplicationRunner {

    private static final String STOCK_KEY = "stock:";

    private final ProductRepository productRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> products = productRepository.findAll();

        for (Product product : products) {
            String key = STOCK_KEY + product.getId();
            String existing = redisTemplate.opsForValue().get(key);

            // 이미 Redis에 값이 있으면 스킵 (재시작 시 중복 동기화 방지)
            if (existing != null) continue;

            redisTemplate.opsForValue().set(key, String.valueOf(product.getStockQuantity()));

            stockHistoryRepository.save(StockHistory.builder()
                    .productId(product.getId())
                    .changeType(StockChangeType.SYNC)
                    .beforeQuantity(0)
                    .afterQuantity(product.getStockQuantity())
                    .changedBy("SYSTEM")
                    .reason("애플리케이션 시작 시 Redis 재고 동기화")
                    .build());
        }

        log.info("[StockSyncRunner] {}개 상품 재고를 Redis에 동기화 완료", products.size());
    }
}
