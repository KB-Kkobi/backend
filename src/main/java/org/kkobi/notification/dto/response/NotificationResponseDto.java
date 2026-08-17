package org.kkobi.notification.dto.response;

import lombok.Data;
import org.kkobi.notification.enums.NotificationType;

import java.time.LocalDateTime;

@Data
public class NotificationResponseDto {

    // 알림 ID
    private Long notificationId;

    // 알림 종류
    private NotificationType type;

    // 알림 제목
    private String title;

    // 알림 내용
    private String message;

    // 원본 데이터 ID
    private Long referenceId;

    // 읽음 여부
    private Boolean read;

    // 읽음 시간
    private LocalDateTime readAt;

    // 알림 생성 시간
    private LocalDateTime createdAt;
}
