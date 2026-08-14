package org.kkobi.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizAnswerResponseDto {

    private final boolean correct;
    private final String answer;
    private final String explanation;
    private final Long rewardAmount;
}