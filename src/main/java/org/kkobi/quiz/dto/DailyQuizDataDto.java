package org.kkobi.quiz.dto;

import lombok.Data;

import java.util.List;

@Data
public class DailyQuizDataDto {

    private Long rewardAmount;
    private List<QuizItemDto> quizzes;
}
