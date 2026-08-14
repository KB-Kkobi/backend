package org.kkobi.quiz.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface QuizMapper {
    
    LocalDate findLastQuizDateByUserId(@Param("userId") Long userId);
    
    Long findAccountIdByUserId(@Param("userId") Long userId);
    
    int markQuizParticipationWithReward(
            @Param("userId") Long userId,
            @Param("rewardAmount") Long rewardAmount,
            @Param("today") LocalDate today
    );
    
    int markQuizParticipationOnly(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );
    
    int insertQuizRewardTransaction(
            @Param("accountId") Long accountId,
            @Param("amount") Long amount,
            @Param("now") LocalDateTime now
    );
}