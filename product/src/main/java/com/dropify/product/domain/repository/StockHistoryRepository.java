package com.dropify.product.domain.repository;

import com.dropify.product.domain.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    List<StockHistory> findByProductIdOrderByCreatedAtDesc(Long productId);
}
