package com.dropify.product.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.product.aop.StockChange;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.entity.StockChangeType;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.dto.request.StockReplenishRequest;
import com.dropify.product.dto.response.StockReplenishResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private static final String STOCK_KEY = "stock:";

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;

    // 주문 모듈에서 호출 — 재고 차감
    @StockChange(type = StockChangeType.DECREASE, reason = "주문 처리")
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = findProduct(productId);
        product.decreaseStock(quantity);
        redisTemplate.opsForValue().decrement(STOCK_KEY + productId, quantity);
    }

    // 관리자 API에서 호출 — 재고 보충
    @StockChange(type = StockChangeType.REPLENISH)
    @Transactional
    public StockReplenishResponse replenishStock(Long productId, StockReplenishRequest request) {
        Product product = findProduct(productId);
        product.increaseStock(request.getQuantity());
        redisTemplate.opsForValue().increment(STOCK_KEY + productId, request.getQuantity());
        return new StockReplenishResponse(product.getId(), product.getStockQuantity());
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
