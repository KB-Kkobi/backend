package org.kkobi.notification.dto.request;

import lombok.Data;

@Data
public class NotificationSettingsUpdateRequestDto {

    // 매수, 매도 체결 알림 수신 여부
    private Boolean tradeEnabled;

    // 친구 관련 알림 수신 여부
    private Boolean friendEnabled;
}
