package org.kkobi.leaderboard.dto;

import lombok.Data;

@Data
public class LeaderboardSecurityHoldingDto {

    // 사용자 ID
    private Long userId;

    // 종목 ID
    private Long securityId;

    // 종목 티커
    private String ticker;

    // KIS 종목 코드
    private String kisCode;

    // 보유 수량
    private Long quantity;
}
