package com.dropify.usecase;

import com.dropify.order.exception.PaymentConfirmFailedException;
import com.dropify.order.dto.request.PaymentConfirmRequest;
import com.dropify.order.dto.response.PaymentConfirmResponse;
import com.dropify.order.service.OrderService;
import com.dropify.order.service.PaymentService;
import com.dropify.order.usecase.PaymentConfirmUseCase;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentConfirmUseCaseImpl implements PaymentConfirmUseCase {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final StockService stockService;

    @Override
    @Transactional(noRollbackFor = PaymentConfirmFailedException.class)
    public PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request) {
        try {
            return paymentService.confirm(userId, request);
        } catch (PaymentConfirmFailedException e) {
            orderService.getOrderItems(request.getOrderId()).forEach(item ->
                    stockService.rollbackStock(item.getProductId(), item.getQuantity()));
            throw e;
        }
    }
}
