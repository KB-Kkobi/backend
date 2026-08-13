package org.kkobi.leaderboard.dto;

import lombok.Data;

import java.util.List;

@Data
public class LeaderboardResponseDto {

    // 로그인 사용자의 현재 순위
    private Long myRank;

    // 로그인 사용자의 투자 성향 ID
    private Long personaId;

    // 로그인 사용자의 투자 성향 이름
    private String personaName;

    // 리더보드 목록
     private List<LeaderboardItemResponseDto> rankings;
}
