package org.kkobi.leaderboard.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaderboardAccountDto {

    // 사용자 ID
    private Long userId;

    // 계좌 ID
    private Long accountId;

    // 사용자 닉네임
    private String nickname;

    // 투자 성향 ID
    private Long personaId;

    // 투자 성향 이름
    private String personaName;

    // 초기 투자금
    private BigDecimal seedMoney;

    // 현재 보유 원금
    private BigDecimal cashBalance;
}
