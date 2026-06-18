package com.dropify.product.aop;

import com.dropify.product.domain.entity.StockHistory;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.domain.repository.StockHistoryRepository;
import com.dropify.product.dto.request.StockReplenishRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
public class StockChangeAspect {

    private final ProductRepository productRepository;
    private final StockHistoryRepository stockHistoryRepository;

    @Around("@annotation(stockChange)")
    public Object recordStockChange(ProceedingJoinPoint pjp, StockChange stockChange) throws Throwable {
        Long productId = extractProductId(pjp);

        int beforeQty = currentStock(productId);

        Object result = pjp.proceed();

        int afterQty = currentStock(productId);

        stockHistoryRepository.save(StockHistory.builder()
                .productId(productId)
                .changeType(stockChange.type())
                .beforeQuantity(beforeQty)
                .afterQuantity(afterQty)
                .changedBy(resolveChangedBy())
                .reason(resolveReason(pjp, stockChange.reason()))
                .build());

        return result;
    }

    private Long extractProductId(ProceedingJoinPoint pjp) {
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof Long id) return id;
        }
        throw new IllegalStateException("@StockChange 메서드의 첫 번째 파라미터는 Long productId 여야 합니다.");
    }

    private int currentStock(Long productId) {
        return productRepository.findById(productId)
                .map(p -> p.getStockQuantity())
                .orElse(0);
    }

    private String resolveReason(ProceedingJoinPoint pjp, String annotationReason) {
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof StockReplenishRequest request && request.getReason() != null) {
                return request.getReason();
            }
        }
        return annotationReason.isBlank() ? null : annotationReason;
    }

    private String resolveChangedBy() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "SYSTEM";
        }
        return auth.getName();
    }
}
