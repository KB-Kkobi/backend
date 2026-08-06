package org.kkobi.game.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.service.GameAssessmentService;
import org.kkobi.game.dto.GameStatusResponse;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameController {

    private final GameAssessmentService gameAssessmentService;

    @GetMapping("/status")
    public ResponseEntity<GameStatusResponse> getGameStatus(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        boolean completed = gameAssessmentService.existsCompletedGame(
                authenticatedUser.getUserId()
        );
        return ResponseEntity.ok(new GameStatusResponse(completed));
    }
}
