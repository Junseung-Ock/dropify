package com.dropify.notification.dto.response;

import com.dropify.notification.domain.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "주문 완료")
    private String title;

    @Schema(example = "주문 #1이 성공적으로 완료되었습니다.")
    private String message;

    @Schema(example = "false")
    private boolean isRead;

    @Schema(example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
