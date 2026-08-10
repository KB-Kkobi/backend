package org.kkobi.game.dto;

import lombok.Data;

@Data
public class ScenarioTickDto {

    private int tick;
    private String date;
    private int month;
    private double marketIndex;
    private long price;
    private double changeRate;
}