package org.kkobi.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.service.GameAssessmentService;
import org.kkobi.game.dto.GameActionRequest;
import org.kkobi.game.dto.GameActionResponse;
import org.kkobi.game.dto.GameCompletionResponse;
import org.kkobi.game.dto.GameStartRequest;
import org.kkobi.game.dto.GameStartResponse;
import org.kkobi.game.dto.GameStatusResponse;
import org.kkobi.game.service.GameActionService;
import org.kkobi.game.service.GameStartService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
@Tag(name = "성향 파악 게임", description = "투자 성향 파악 게임 진행 API")
public class GameController {

    private final GameAssessmentService gameAssessmentService;
    private final GameStartService gameStartService;
    private final GameActionService gameActionService;

    @Operation(
            summary = "게임 상태 조회",
            description = "로그인한 사용자의 현재 게임 진행 상태를 조회합니다."
    )
    @GetMapping("/status")
    public ResponseEntity<GameStatusResponse> getGameStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        boolean completed = gameAssessmentService.existsCompletedGame(
                authenticatedUser.getUserId()
        );
        return ResponseEntity.ok(new GameStatusResponse(completed));
    }

    @Operation(
            summary = "게임 시작",
            description = "투자 성향 파악 게임을 시작합니다."
    )
    @PostMapping("/start")
    public ResponseEntity<GameStartResponse> startGame(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @Valid @RequestBody GameStartRequest request) {
        return ResponseEntity.ok(gameStartService.startGame(
                authenticatedUser.getUserId(),
                request
        ));
    }

    @Operation(
            summary = "게임 행동 저장",
            description = "게임 진행 중 사용자가 선택한 행동을 저장합니다."
    )
    @PostMapping("/actions")
    public ResponseEntity<GameActionResponse> saveGameAction(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @Valid @RequestBody GameActionRequest request) {
        return ResponseEntity.ok(gameActionService.saveGameAction(
                authenticatedUser.getUserId(),
                request
        ));
    }

    @Operation(
            summary = "게임 완료",
            description = "게임을 완료하고 투자 성향 결과를 생성합니다."
    )
    @PostMapping("/completion")
    public ResponseEntity<GameCompletionResponse> completeGame(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        return ResponseEntity.ok(gameAssessmentService.completeGame(
                authenticatedUser.getUserId()
        ));
    }
}
