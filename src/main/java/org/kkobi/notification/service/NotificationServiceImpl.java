package org.kkobi.notification.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.notification.dto.request.NotificationSettingsUpdateRequestDto;
import org.kkobi.notification.dto.response.NotificationResponseDto;
import org.kkobi.notification.dto.response.NotificationSettingsResponseDto;
import org.kkobi.notification.enums.NotificationType;
import org.kkobi.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{

    private final NotificationMapper notificationMapper;

    // 사용자 알림 설정을 조회
    @Override
    @Transactional(readOnly = true)
    public NotificationSettingsResponseDto getSettings(Long userId) {

        NotificationSettingsResponseDto settings = notificationMapper.findSettings(userId);

        // 별도 설정이 없는 사용자는 모든 알림을 기본 허용
        if(settings == null){
            settings = new NotificationSettingsResponseDto();
            settings.setTradeEnabled(true);
            settings.setFriendEnabled(true);
        }

        return settings;
    }

    // 사용자 알림 설정을 수정
    @Override
    @Transactional
    public void updateSettings(Long userId, NotificationSettingsUpdateRequestDto request){
        if(request.getFriendEnabled() == null && request.getTradeEnabled() == null){
            throw new IllegalArgumentException("변경할 알림 설정이 없습니다.");
        }

        NotificationSettingsResponseDto currentSettings = getSettings(userId);

        boolean tradeEnabled = request.getTradeEnabled() != null ? request.getTradeEnabled() : currentSettings.isTradeEnabled();

        boolean friendEnabled = request.getFriendEnabled() != null ? request.getTradeEnabled() : currentSettings.isTradeEnabled();

        int updatedRows = notificationMapper.upsertSettings(
                userId,
                tradeEnabled,
                friendEnabled
        );
    }

    // 사용자 알림 목록을 조회
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long userId){
        return notificationMapper.findNotifications(userId);
    }

    // 읽지 않은 알림 개수를 조회
    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId){
        return notificationMapper.countUnreadNotifications(userId);
    }

    // 개별 알림을 읽을 처리
    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId){
        notificationMapper.markAsRead(userId, notificationId);
    }

    // 모든 알림을 읽음 처리
    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    // 사용자 설정에 따라 신규 알림을 생성
    @Override
    @Transactional
    public void createNotification(
            Long userId,
            NotificationType type,
            String title,
            String message,
            Long referenceId
    ) {
        NotificationSettingsResponseDto settings = getSettings(userId);

        if(!isNotificationEnabled(settings, type)) {
            return;
        }

        int insertedRows = notificationMapper.insertNotification(
                userId,
                type,
                title,
                message,
                referenceId
        );

        if(insertedRows != 1){
            throw new IllegalArgumentException("알림 저장에 실패했습니다.");
        }
    }

    // 알림 종류에 따라 사용자 수신 설정을 확인
    private boolean isNotificationEnabled(
            NotificationSettingsResponseDto settings,
            NotificationType type
    ) {
        switch (type) {
            case TRADE_BUY_FILLED:
            case TRADE_SELL_FILLED:
                return settings.isTradeEnabled();

            case FRIEND_REQUEST_RECEIVED:
            case FRIEND_REQUEST_ACCEPTED:
                return settings.isFriendEnabled();

            default:
                return false;
        }
    }

    // 현재 사용쟈의 모든 알림 삭제
    @Override
    @Transactional
    public void deleteAllNotifications(Long userId) {

        notificationMapper.deleteAllNotifications(userId);
    }
}
