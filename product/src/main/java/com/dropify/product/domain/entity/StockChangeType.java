package com.dropify.product.domain.entity;

public enum StockChangeType {
    DECREASE,   // 주문으로 인한 재고 차감
    REPLENISH,  // 관리자 재고 보충
    SYNC        // Redis 초기 동기화
}
