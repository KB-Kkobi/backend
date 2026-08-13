package org.kkobi.leaderboard.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaderboardCacheDto {

    // 사용자 ID
    private Long userId;

    // 사용자 닉네임
    private String nickname;

    // 투자 성향 ID
    private Long personaId;

    // 투자 성향 이름
    private String personaName;

    // 현재 총자산
    private BigDecimal totalAsset;

    // 초기 투자금 대비 수익율
    private BigDecimal returnRate;

}
