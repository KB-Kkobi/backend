package org.kkobi.game.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScenarioDto {

    private String scenarioId;
    private int totalTicks;
    private long basePrice;
    private int priceMultiplier;
    private long tickIntervalMs;
    private List<ScenarioTickDto> ticks;
    private List<ScenarioEventDto> events;
}