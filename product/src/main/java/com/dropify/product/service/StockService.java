package com.dropify.product.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.event.StockChangedEvent;
import com.dropify.product.aop.StockChange;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.entity.StockChangeType;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.dto.request.StockReplenishRequest;
import com.dropify.product.dto.response.StockReplenishResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class StockService {

    private static final String STOCK_KEY = "stock:";

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // 주문 모듈에서 호출 — 재고 차감
    @StockChange(type = StockChangeType.DECREASE, reason = "주문 처리")
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = findProduct(productId);
        initRedisKeyIfAbsent(productId, product.getStockQuantity());
        product.decreaseStock(quantity);
        int newQuantity = product.getStockQuantity();
        eventPublisher.publishEvent(
                StockChangedEvent.builder().productId(productId).quantity(newQuantity).build()
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.opsForValue().decrement(STOCK_KEY + productId, quantity);
            }
        });
    }

    // 관리자 API에서 호출 — 재고 보충
    @StockChange(type = StockChangeType.REPLENISH)
    @Transactional
    public StockReplenishResponse replenishStock(Long productId, StockReplenishRequest request) {
        Product product = findProduct(productId);
        initRedisKeyIfAbsent(productId, product.getStockQuantity());
        product.increaseStock(request.getQuantity());
        int newQuantity = product.getStockQuantity();
        eventPublisher.publishEvent(
                StockChangedEvent.builder().productId(productId).quantity(newQuantity).build()
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.opsForValue().increment(STOCK_KEY + productId, request.getQuantity());
            }
        });
        return new StockReplenishResponse(product.getId(), product.getStockQuantity());
    }

    // 결제 실패 시 호출 — 이미 커밋된 재고 차감을 보상하는 별도 트랜잭션
    @StockChange(type = StockChangeType.ROLLBACK, reason = "결제 실패 재고 복구")
    @Transactional
    public void rollbackStock(Long productId, int quantity) {
        Product product = findProduct(productId);
        initRedisKeyIfAbsent(productId, product.getStockQuantity());
        product.increaseStock(quantity);
        int newQuantity = product.getStockQuantity();
        eventPublisher.publishEvent(
                StockChangedEvent.builder().productId(productId).quantity(newQuantity).build()
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.opsForValue().increment(STOCK_KEY + productId, quantity);
            }
        });
    }

    public void checkRedisStock(Long productId, int quantity) {
        String stock = redisTemplate.opsForValue().get(STOCK_KEY + productId);
        if (stock != null && Integer.parseInt(stock) < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private void initRedisKeyIfAbsent(Long productId, int stockQuantity) {
        redisTemplate.opsForValue().setIfAbsent(STOCK_KEY + productId, String.valueOf(stockQuantity));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
