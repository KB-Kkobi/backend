package org.kkobi.notification.service;

import org.kkobi.notification.dto.request.NotificationSettingsUpdateRequestDto;
import org.kkobi.notification.dto.response.NotificationResponseDto;
import org.kkobi.notification.dto.response.NotificationSettingsResponseDto;
import org.kkobi.notification.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    // 사용자 알림 설정을 조회
    NotificationSettingsResponseDto getSettings(Long userId);

    // 사용자 알림 설정을 수정
    void updateSettings(
            Long userId,
            NotificationSettingsUpdateRequestDto request
    );

    // 사용자 알림 목록을 조회
    List<NotificationResponseDto> getNotifications(Long userId);

    // 읽지 않은 알림 개수를 조회
    int getUnreadCount(Long userId);

    // 개별 알림을 읽음 처리
    void markAsRead(Long userId, Long notificationId);

    // 모든 알림을 읽음 처리
    void markAllAsRead(Long userId);

    // 사용자 설정에 따라 신규 알림을 생성
    void createNotification(
            Long userId,
            NotificationType type,
            String title,
            String message,
            Long referenceId
    );

    // 현재 사용자의 모든 알림 삭제
    void deleteAllNotifications(Long userId);

    // 특정 참조 데이터와 연결된 알림 삭제
    void deleteNotificationByReference(
            Long userId,
            NotificationType type,
            Long referenceId
    );
}
