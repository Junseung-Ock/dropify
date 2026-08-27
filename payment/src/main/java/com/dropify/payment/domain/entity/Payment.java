package com.dropify.payment.domain.entity;

import com.dropify.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(length = 200, unique = true)
    private String tossPaymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Long amount;

    private LocalDateTime paidAt;

    @Builder
    private Payment(Long orderId, Long amount) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public boolean complete(String tossPaymentKey) {
        if (this.status != PaymentStatus.PENDING) return false;
        this.tossPaymentKey = tossPaymentKey;
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
        return true;
    }

    public boolean fail() {
        if (this.status != PaymentStatus.PENDING) return false;
        this.status = PaymentStatus.FAILED;
        return true;
    }

    public boolean cancel() {
        if (this.status != PaymentStatus.COMPLETED) return false;
        this.status = PaymentStatus.CANCELLED;
        return true;
    }
}
