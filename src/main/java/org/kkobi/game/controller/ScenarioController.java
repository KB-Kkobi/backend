package org.kkobi.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.service.ScenarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games/scenarios")
@Tag(name = "게임 시나리오", description = "게임 시나리오 조회 API")
public class ScenarioController {

    private final ScenarioService scenarioService;

    @Operation(
            summary = "게임 시나리오 조회",
            description = "시나리오 ID를 기준으로 게임 시나리오 정보를 조회합니다."
    )
    @GetMapping("/{scenarioId}")
    public ScenarioDto getScenario(@PathVariable String scenarioId) {
        return scenarioService.getScenario(scenarioId);
    }
}