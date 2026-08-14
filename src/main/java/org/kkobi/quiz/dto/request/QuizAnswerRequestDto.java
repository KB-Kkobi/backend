package org.kkobi.quiz.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class QuizAnswerRequestDto {

    @NotBlank
    @Pattern(regexp = "O|X", message = "답안은 O 또는 X여야 합니다.")
    private String answer;
}