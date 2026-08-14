package org.kkobi.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TodayQuizResponseDto {

    private final String date;
    private final String category;
    private final String question;
    private final boolean hasQuizToday;
    private final boolean hasParticipatedToday;
    private final boolean canParticipate;
}