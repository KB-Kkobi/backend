package org.kkobi.game.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GameStatusResponse {

    private final boolean completed;

    public GameStatusResponse(boolean completed) {
        this.completed = completed;
    }

    @JsonProperty("isCompleted")
    public boolean isCompleted() {
        return completed;
    }
}
