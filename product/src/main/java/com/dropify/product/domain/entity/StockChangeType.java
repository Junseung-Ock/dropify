package com.dropify.product.domain.entity;

public enum StockChangeType {
    DECREASE,   // 주문으로 인한 재고 차감
    REPLENISH,  // 관리자 재고 보충
    ROLLBACK,   // 결제 실패로 인한 재고 복구
    SYNC        // Redis 초기 동기화
}
