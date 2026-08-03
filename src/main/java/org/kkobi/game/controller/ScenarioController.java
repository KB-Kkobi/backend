package org.kkobi.game.controller;

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
public class ScenarioController {

    private final ScenarioService scenarioService;

    @GetMapping("/{scenarioId}")
    public ScenarioDto getScenario(@PathVariable String scenarioId) {
        return scenarioService.getScenario(scenarioId);
    }
}