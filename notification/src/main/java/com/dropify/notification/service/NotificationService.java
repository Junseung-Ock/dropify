package com.dropify.notification.service;

import com.dropify.notification.domain.entity.Notification;
import com.dropify.notification.domain.entity.NotificationType;
import com.dropify.notification.domain.repository.NotificationRepository;
import com.dropify.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void savePaymentCompletedNotification(Long userId, Long orderId) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.PUSH)
                .title("결제 완료")
                .message("주문 #" + orderId + " 결제가 완료되었습니다.")
                .build());
        log.info("결제 완료 알림 저장: userId={}, orderId={}", userId, orderId);
    }

    @Transactional
    public void savePaymentFailedNotification(Long userId, Long orderId) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.PUSH)
                .title("결제 실패")
                .message("주문 #" + orderId + " 결제에 실패하였습니다.")
                .build());
        log.info("결제 실패 알림 저장: userId={}, orderId={}", userId, orderId);
    }

    @Transactional
    public void saveOrderCancelledNotification(Long userId, Long orderId) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.PUSH)
                .title("주문 취소")
                .message("주문 #" + orderId + " 이 취소되었습니다.")
                .build());
        log.info("주문 취소 알림 저장: userId={}, orderId={}", userId, orderId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
