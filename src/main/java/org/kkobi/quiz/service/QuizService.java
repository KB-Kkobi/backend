package org.kkobi.quiz.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.account.mapper.AccountMapper;
import org.kkobi.quiz.dto.DailyQuizDataDto;
import org.kkobi.quiz.dto.QuizItemDto;
import org.kkobi.quiz.dto.request.QuizAnswerRequestDto;
import org.kkobi.quiz.dto.response.QuizAnswerResponseDto;
import org.kkobi.quiz.dto.response.TodayQuizResponseDto;
import org.kkobi.quiz.mapper.QuizMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class QuizService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuizDataService quizDataService;
    private final QuizMapper quizMapper;
    private final AccountMapper accountMapper;

    @Transactional(readOnly = true)
    public TodayQuizResponseDto getTodayQuiz(Long userId) {
        return getTodayQuiz(userId, LocalDate.now(KST));
    }

    TodayQuizResponseDto getTodayQuiz(Long userId, LocalDate today) {
        DailyQuizDataDto quizData = quizDataService.loadDailyQuizData();
        QuizItemDto quiz = findQuizByDate(quizData, today);
        boolean hasQuizToday = quiz != null;

        boolean hasAccount = accountMapper.existsAccountByUserId(userId);
        boolean hasParticipatedToday = false;
        if (hasAccount) {
            LocalDate lastQuizDate = quizMapper.findLastQuizDateByUserId(userId);
            hasParticipatedToday = today.equals(lastQuizDate);
        }

        boolean canParticipate = hasAccount && hasQuizToday && !hasParticipatedToday;

        return new TodayQuizResponseDto(
                hasQuizToday ? quiz.getDate() : null,
                hasQuizToday ? quiz.getCategory() : null,
                hasQuizToday ? quiz.getQuestion() : null,
                hasQuizToday,
                hasParticipatedToday,
                canParticipate,
                hasParticipatedToday ? quiz.getAnswer() : null,
                hasParticipatedToday ? quiz.getExplanation() : null
        );
    }

    @Transactional
    public QuizAnswerResponseDto submitAnswer(Long userId, QuizAnswerRequestDto request) {
        return submitAnswer(userId, request, LocalDate.now(KST), LocalDateTime.now(KST));
    }

    QuizAnswerResponseDto submitAnswer(
            Long userId,
            QuizAnswerRequestDto request,
            LocalDate today,
            LocalDateTime now
    ) {
        if (!accountMapper.existsAccountByUserId(userId)) {
            throw new IllegalArgumentException("가상 투자 계좌가 없어 퀴즈에 참여할 수 없습니다.");
        }

        DailyQuizDataDto quizData = quizDataService.loadDailyQuizData();
        QuizItemDto quiz = findQuizByDate(quizData, today);
        if (quiz == null) {
            throw new IllegalArgumentException("오늘의 퀴즈가 없습니다.");
        }

        Long rewardAmount = quizData.getRewardAmount();
        boolean correct = quiz.getAnswer().equals(request.getAnswer());

        int updatedRows = correct
                ? quizMapper.markQuizParticipationWithReward(userId, rewardAmount, today)
                : quizMapper.markQuizParticipationOnly(userId, today);

        if (updatedRows != 1) {
            throw new IllegalArgumentException("오늘은 이미 퀴즈에 참여했습니다.");
        }

        if (correct) {
            Long accountId = quizMapper.findAccountIdByUserId(userId);
            quizMapper.insertQuizRewardTransaction(accountId, rewardAmount, now);
        }

        return new QuizAnswerResponseDto(
                correct,
                quiz.getAnswer(),
                quiz.getExplanation(),
                correct ? rewardAmount : 0L
        );
    }

    private QuizItemDto findQuizByDate(DailyQuizDataDto data, LocalDate date) {
        return data.getQuizzes().stream()
                .filter(quiz -> date.equals(LocalDate.parse(quiz.getDate())))
                .findFirst()
                .orElse(null);
    }
}