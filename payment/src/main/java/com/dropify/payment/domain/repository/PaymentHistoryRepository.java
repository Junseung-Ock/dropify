package com.dropify.payment.domain.repository;

import com.dropify.payment.domain.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    List<PaymentHistory> findByUserIdOrderByPaidAtDesc(Long userId);

    Optional<PaymentHistory> findByOrderId(Long orderId);
}
