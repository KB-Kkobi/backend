package org.kkobi.game.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameStatusResponseTest {

    @Test
    @DisplayName("게임 완료 여부를 isCompleted 필드로 응답한다.")
    void serializeGameStatusResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String responseBody = objectMapper.writeValueAsString(
                new GameStatusResponse(true)
        );

        assertEquals("{\"isCompleted\":true}", responseBody);
    }
}
