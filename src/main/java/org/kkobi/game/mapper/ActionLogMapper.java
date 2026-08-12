package org.kkobi.game.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.game.dto.ActionLogDto;

import java.util.List;

public interface ActionLogMapper {

    // 게임 행동 로그 저장
    int saveActionLog(ActionLogDto actionLog);

    // 사용자별 게임 행동 로그 조회
    List<ActionLogDto> getActionLogsByUserId(@Param("userId") Long userId);

    boolean existsCompletedGame(@Param("userId") Long userId);

    Long lockUserById(@Param("userId") Long userId);

    int deleteActionLogsByUserId(@Param("userId") Long userId);
}
