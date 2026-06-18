package com.dropify.product.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockChangeType changeType;

    @Column(nullable = false)
    private int beforeQuantity;

    @Column(nullable = false)
    private int afterQuantity;

    // 양수: 증가 / 음수: 감소
    @Column(nullable = false)
    private int changeQuantity;

    @Column(nullable = false, length = 100)
    private String changedBy;

    @Column(length = 255)
    private String reason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private StockHistory(Long productId, StockChangeType changeType,
                         int beforeQuantity, int afterQuantity,
                         String changedBy, String reason) {
        this.productId = productId;
        this.changeType = changeType;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.changeQuantity = afterQuantity - beforeQuantity;
        this.changedBy = changedBy;
        this.reason = reason;
    }
}
