package org.kkobi.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kkobi.quiz.dto.DailyQuizDataDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class QuizDataService {

    private static final String QUIZ_RESOURCE_PATH = "org/kkobi/quiz/data/DailyQuiz.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DailyQuizDataDto loadDailyQuizData() {
        ClassPathResource resource = new ClassPathResource(QUIZ_RESOURCE_PATH);

        if (!resource.exists()) {
            throw new IllegalStateException("퀴즈 데이터 파일을 찾을 수 없습니다: " + QUIZ_RESOURCE_PATH);
        }

        try {
            return objectMapper.readValue(resource.getInputStream(), DailyQuizDataDto.class);
        } catch (IOException e) {
            throw new IllegalStateException("퀴즈 데이터를 읽는 중 오류가 발생했습니다.", e);
        }
    }
}