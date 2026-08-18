package org.kkobi.notification.dto.response;

import lombok.Data;

@Data
public class NotificationSettingsResponseDto {

    // 매수, 매도 체결 알림 수신 여부
    private boolean tradeEnabled;

    // 친구 관련 알림 수신 여부
    private boolean friendEnabled;
}
