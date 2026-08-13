package org.kkobi.leaderboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.leaderboard.dto.LeaderboardResponseDto;
import org.kkobi.leaderboard.service.LeaderboardService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leaderboard")
@Tag(name = "리더보드", description = "사용자 수익률 기반 리더보드 API")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    // 로그인 사용자와 같은 투자 성향의 리더보드를 조회
    @Operation(
            summary = "성향별 리더보드 조회",
            description = "로그인 사용자와 같은 투자 성향의 사용자들을 수익률 순으로 조회합니다."
    )
    @GetMapping("/persona")
    public ResponseEntity<LeaderboardResponseDto> getPersonaLeaderbboard(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
            ){
        LeaderboardResponseDto response = leaderboardService.getPersonaLeaderboard(authenticatedUser.getUserId());

        return ResponseEntity.ok(response);
    }

    // 로그인 사용자와 친구들의 리더보드를 조회
    @Operation(
            summary = "친구별 리더보드 조회",
            description = "로그인 사용자와 친구들을 수익률 순으로 조회합니다."
    )
    @GetMapping("/friends")
    public ResponseEntity<LeaderboardResponseDto> getFriendLeaderboard(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ){
        LeaderboardResponseDto response = leaderboardService.getFriendLeaderboard(authenticatedUser.getUserId());

        return ResponseEntity.ok(response);
    }
}
