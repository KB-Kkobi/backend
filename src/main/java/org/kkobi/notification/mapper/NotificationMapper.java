package org.kkobi.notification.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.notification.dto.response.NotificationResponseDto;
import org.kkobi.notification.dto.response.NotificationSettingsResponseDto;
import org.kkobi.notification.enums.NotificationType;

import java.util.List;

public interface NotificationMapper {

    // 사용자의 알림 설정을 조회
    NotificationSettingsResponseDto findSettings(
            @Param("userId") Long userId
    );

    // 사용자의 알림 설정을 저장하거나 수정
    int upsertSettings(
            @Param("userId") Long userId,
            @Param("tradeEnabled") boolean tradeEnabled,
            @Param("friendEnabled") boolean friendEnabled
    );

    // 사용자의 알림 목록을 최신순으로 조회
    List<NotificationResponseDto> findNotifications(
            @Param("userId") Long userId
    );

    // 읽지 않은 알림 개수를 조회
    int countUnreadNotifications(
            @Param("userId") Long userId
    );

    // 개별 알림을 읽음 처리
    int markAsRead(
            @Param("userId") Long userId,
            @Param("notificationId") Long notificationId
    );

    // 사용자의 모든 알림을 읽음 처리
    int markAllAsRead(
            @Param("userId") Long userId
    );

    // 새로운 알림을 저장
    int insertNotification(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("title") String title,
            @Param("message") String message,
            @Param("referenceId") Long referenceId
    );

    // 현재 사용자의 모든 알림 삭제
    int deleteAllNotifications(
            @Param("userId") Long userId
    );
}
