package org.kkobi.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.quiz.dto.request.QuizAnswerRequestDto;
import org.kkobi.quiz.dto.response.QuizAnswerResponseDto;
import org.kkobi.quiz.dto.response.TodayQuizResponseDto;
import org.kkobi.quiz.service.QuizService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quizzes")
@Tag(name = "일일 금융 퀴즈", description = "하루 1회 O/X 금융 퀴즈 API")
public class QuizController {

    private final QuizService quizService;

    @Operation(
            summary = "오늘의 퀴즈 조회",
            description = "오늘 날짜의 퀴즈와 참여 가능 여부를 조회합니다."
    )
    @GetMapping("/today")
    public ResponseEntity<TodayQuizResponseDto> getTodayQuiz(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        return ResponseEntity.ok(
                quizService.getTodayQuiz(authenticatedUser.getUserId())
        );
    }

    @Operation(
            summary = "퀴즈 답안 제출",
            description = "오늘의 퀴즈에 O 또는 X로 답안을 제출합니다."
    )
    @PostMapping("/today/answer")
    public ResponseEntity<QuizAnswerResponseDto> submitAnswer(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @Valid @RequestBody QuizAnswerRequestDto request
    ) {
        return ResponseEntity.ok(
                quizService.submitAnswer(authenticatedUser.getUserId(), request)
        );
    }
}