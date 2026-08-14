package org.kkobi.quiz.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RootConfig;
import org.kkobi.security.config.SecurityConfig;
import org.kkobi.quiz.dto.request.QuizAnswerRequestDto;
import org.kkobi.quiz.dto.response.QuizAnswerResponseDto;
import org.kkobi.quiz.dto.response.TodayQuizResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, SecurityConfig.class})
@Transactional
class QuizServiceTest {

    private static final LocalDate QUIZ_DATE = LocalDate.of(2026, 8, 14);
    private static final LocalDate NO_QUIZ_DATE = LocalDate.of(2099, 1, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 14, 12, 0);

    @Autowired
    private QuizService quizService;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(DataSource ds) {
        jdbcTemplate = new JdbcTemplate(ds);
    }

    private Long createUserAndAccount(long cashBalance) {
        String identifier = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "quiz-" + identifier + "@test.com";

        jdbcTemplate.update(
                "INSERT INTO users (email, password, nickname, birth_date, postal_code, address_line1) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                email, "password", "quiz-" + identifier,
                "2000-01-01", "00000", "테스트 주소"
        );
        Long userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?", Long.class, email);

        jdbcTemplate.update(
                "INSERT INTO accounts (user_id, seed_money, cash_balance, locked_cash) "
                        + "VALUES (?, ?, ?, ?)",
                userId, cashBalance, cashBalance, 0L
        );
        return userId;
    }

    private Long createUserWithoutAccount() {
        String identifier = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "quiz-noacct-" + identifier + "@test.com";

        jdbcTemplate.update(
                "INSERT INTO users (email, password, nickname, birth_date, postal_code, address_line1) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                email, "password", "quiz-noacct-" + identifier,
                "2000-01-01", "00000", "테스트 주소"
        );
        return jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    private QuizAnswerRequestDto answerOf(String answer) {
        QuizAnswerRequestDto request = new QuizAnswerRequestDto();
        request.setAnswer(answer);
        return request;
    }

    @Test
    @DisplayName("계좌가 없으면 문제는 보이되 참여는 불가능하다")
    void getTodayQuiz_noAccount_cannotParticipate() {
        Long userId = createUserWithoutAccount();

        TodayQuizResponseDto response = quizService.getTodayQuiz(userId, QUIZ_DATE);

        assertTrue(response.isHasQuizToday());
        assertFalse(response.isHasParticipatedToday());
        assertFalse(response.isCanParticipate());
    }

    @Test
    @DisplayName("계좌가 있고 미참여 상태면 참여 가능하다")
    void getTodayQuiz_hasAccountNotParticipated_canParticipate() {
        Long userId = createUserAndAccount(1_000_000L);

        TodayQuizResponseDto response = quizService.getTodayQuiz(userId, QUIZ_DATE);

        assertTrue(response.isHasQuizToday());
        assertFalse(response.isHasParticipatedToday());
        assertTrue(response.isCanParticipate());
    }

    @Test
    @DisplayName("오늘 이미 참여했다면 참여 불가능하다")
    void getTodayQuiz_alreadyParticipated_cannotParticipateAgain() {
        Long userId = createUserAndAccount(1_000_000L);
        quizService.submitAnswer(userId, answerOf("O"), QUIZ_DATE, NOW);

        TodayQuizResponseDto response = quizService.getTodayQuiz(userId, QUIZ_DATE);

        assertTrue(response.isHasParticipatedToday());
        assertFalse(response.isCanParticipate());
    }

    @Test
    @DisplayName("해당 날짜에 퀴즈가 없으면 hasQuizToday가 false다")
    void getTodayQuiz_noQuizForDate_hasQuizTodayFalse() {
        Long userId = createUserAndAccount(1_000_000L);

        TodayQuizResponseDto response = quizService.getTodayQuiz(userId, NO_QUIZ_DATE);

        assertFalse(response.isHasQuizToday());
        assertFalse(response.isCanParticipate());
    }

    @Test
    @DisplayName("계좌가 없으면 답안 제출이 거부된다")
    void submitAnswer_noAccount_throws() {
        Long userId = createUserWithoutAccount();

        assertThrows(IllegalArgumentException.class,
                () -> quizService.submitAnswer(userId, answerOf("O"), QUIZ_DATE, NOW));
    }

    @Test
    @DisplayName("정답을 맞히면 보상이 지급되고 거래 내역이 남는다")
    void submitAnswer_correct_grantsRewardAndRecordsTransaction() {
        Long userId = createUserAndAccount(1_000_000L);

        QuizAnswerResponseDto response =
                quizService.submitAnswer(userId, answerOf("O"), QUIZ_DATE, NOW);

        assertTrue(response.isCorrect());
        assertEquals("O", response.getAnswer());
        assertEquals(50_000L, response.getRewardAmount());

        Long cashBalance = jdbcTemplate.queryForObject(
                "SELECT cash_balance FROM accounts WHERE user_id = ?", Long.class, userId);
        assertEquals(1_050_000L, cashBalance);

        LocalDate lastQuizDate = jdbcTemplate.queryForObject(
                "SELECT last_quiz_date FROM accounts WHERE user_id = ?", LocalDate.class, userId);
        assertEquals(QUIZ_DATE, lastQuizDate);

        Integer transactionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_transactions t "
                        + "JOIN accounts a ON a.account_id = t.account_id "
                        + "WHERE a.user_id = ? AND t.type = 'DEPOSIT' AND t.amount = 50000",
                Integer.class, userId);
        assertEquals(1, transactionCount);
    }

    @Test
    @DisplayName("오답이면 보상 없이 참여일만 기록된다")
    void submitAnswer_incorrect_noRewardOnlyMarksParticipation() {
        Long userId = createUserAndAccount(1_000_000L);

        QuizAnswerResponseDto response =
                quizService.submitAnswer(userId, answerOf("X"), QUIZ_DATE, NOW);

        assertFalse(response.isCorrect());
        assertEquals("O", response.getAnswer());
        assertEquals(0L, response.getRewardAmount());

        Long cashBalance = jdbcTemplate.queryForObject(
                "SELECT cash_balance FROM accounts WHERE user_id = ?", Long.class, userId);
        assertEquals(1_000_000L, cashBalance);

        Integer transactionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_transactions t "
                        + "JOIN accounts a ON a.account_id = t.account_id "
                        + "WHERE a.user_id = ?",
                Integer.class, userId);
        assertEquals(0, transactionCount);
    }

    @Test
    @DisplayName("같은 날 두 번째 제출은 거부된다")
    void submitAnswer_secondSubmissionSameDay_throws() {
        Long userId = createUserAndAccount(1_000_000L);
        quizService.submitAnswer(userId, answerOf("O"), QUIZ_DATE, NOW);

        assertThrows(IllegalArgumentException.class,
                () -> quizService.submitAnswer(userId, answerOf("O"), QUIZ_DATE, NOW));
    }

    @Test
    @DisplayName("해당 날짜에 퀴즈가 없으면 제출이 거부된다")
    void submitAnswer_noQuizForDate_throws() {
        Long userId = createUserAndAccount(1_000_000L);

        assertThrows(IllegalArgumentException.class,
                () -> quizService.submitAnswer(userId, answerOf("O"), NO_QUIZ_DATE, NOW));
    }
}