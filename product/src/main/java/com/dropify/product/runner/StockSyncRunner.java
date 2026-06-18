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
        int syncedCount = 0;

        for (Product product : products) {
            String key = STOCK_KEY + product.getId();
            String existing = redisTemplate.opsForValue().get(key);
            String currentStock = String.valueOf(product.getStockQuantity());

            if (currentStock.equals(existing)) continue;

            int beforeQty = existing != null ? Integer.parseInt(existing) : 0;

            redisTemplate.opsForValue().set(key, currentStock);

            stockHistoryRepository.save(StockHistory.builder()
                    .productId(product.getId())
                    .changeType(StockChangeType.SYNC)
                    .beforeQuantity(beforeQty)
                    .afterQuantity(product.getStockQuantity())
                    .changedBy("SYSTEM")
                    .reason("애플리케이션 시작 시 Redis 재고 동기화")
                    .build());

            syncedCount++;
        }

        log.info("[StockSyncRunner] 전체 {}개 중 {}개 상품 재고를 Redis에 동기화 완료", products.size(), syncedCount);
    }
}
