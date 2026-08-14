package org.kkobi.quiz.dto;

import lombok.Data;

@Data
public class QuizItemDto {

    private String date;
    private String category;
    private String question;
    private String answer;
    private String explanation;
}