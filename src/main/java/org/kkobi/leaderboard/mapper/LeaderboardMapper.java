package org.kkobi.leaderboard.mapper;

import org.kkobi.leaderboard.dto.LeaderboardAccountDto;
import org.kkobi.leaderboard.dto.LeaderboardSecurityHoldingDto;
import org.kkobi.product.holding.dto.ProductHoldingInfoDto;

import java.util.List;

public interface LeaderboardMapper {

    // 리더보드 계산 대상 사용자의 계좌와 최신 투자 성향을 조회
    List<LeaderboardAccountDto> getLeaderboardAccounts();

    // 전체 사용자의 보유 주식 정보를 조회
    List<LeaderboardSecurityHoldingDto> getSecurityHoldings();

    // 전체 사용자의 활성 예적금 보유 정보를 조회
    List<ProductHoldingInfoDto> getActiveProductHoldings();
}
