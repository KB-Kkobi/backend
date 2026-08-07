package org.kkobi.game.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.service.GameAssessmentService;
import org.kkobi.game.dto.GameActionRequest;
import org.kkobi.game.dto.GameActionResponse;
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
public class GameController {

    private final GameAssessmentService gameAssessmentService;
    private final GameStartService gameStartService;
    private final GameActionService gameActionService;

    @GetMapping("/status")
    public ResponseEntity<GameStatusResponse> getGameStatus(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        boolean completed = gameAssessmentService.existsCompletedGame(
                authenticatedUser.getUserId()
        );
        return ResponseEntity.ok(new GameStatusResponse(completed));
    }

    @PostMapping("/start")
    public ResponseEntity<GameStartResponse> startGame(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @Valid @RequestBody GameStartRequest request) {
        return ResponseEntity.ok(gameStartService.startGame(
                authenticatedUser.getUserId(),
                request
        ));
    }

    @PostMapping("/actions")
    public ResponseEntity<GameActionResponse> saveGameAction(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @Valid @RequestBody GameActionRequest request) {
        return ResponseEntity.ok(gameActionService.saveGameAction(
                authenticatedUser.getUserId(),
                request
        ));
    }
}
