package org.kkobi.leaderboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class LeaderboardScheduler {

    private final LeaderboardRefreshService leaderboardRefreshService;

    // 1분마다 전체 리더보드 데이터를 갱신
    @Scheduled(fixedDelay = 60000)
    public void refreshLeaderboard() {
        log.info("리더보드 정기 갱신 시작");

        leaderboardRefreshService.refreshAll();

        log.info("리더보드 정기 갱신 완료");
    }
}
