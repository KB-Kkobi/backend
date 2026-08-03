package org.kkobi.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kkobi.game.dto.ScenarioDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ScenarioService {

    private static final String SCENARIO_RESOURCE_PATH = "org/kkobi/game/scenario/%s.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScenarioDto getScenario(String scenarioId) {
        ClassPathResource resource = new ClassPathResource(
                String.format(SCENARIO_RESOURCE_PATH, scenarioId));

        if (!resource.exists()) {
            throw new IllegalArgumentException("존재하지 않는 시나리오입니다: " + scenarioId);
        }

        try {
            return objectMapper.readValue(resource.getInputStream(), ScenarioDto.class);
        } catch (IOException e) {
            throw new IllegalStateException("시나리오 데이터를 읽는 중 오류가 발생했습니다: " + scenarioId, e);
        }
    }
}