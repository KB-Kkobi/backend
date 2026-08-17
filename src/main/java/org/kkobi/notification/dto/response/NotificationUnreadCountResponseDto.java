package org.kkobi.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationUnreadCountResponseDto {

    // 읽지 않은 알림 개수
    private int unreadCount;
}
