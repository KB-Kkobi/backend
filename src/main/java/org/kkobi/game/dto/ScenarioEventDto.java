package org.kkobi.game.dto;

import lombok.Data;

@Data
public class ScenarioEventDto {

    private int tick;
    private String tag;
    private String tagTone;
    private String summary;
    private String description;
}