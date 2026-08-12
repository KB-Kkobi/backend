package org.kkobi.game.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.mapper.ActionLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionLogService {

    private final ActionLogMapper actionLogMapper;

    public Long saveActionLog(ActionLogDto actionLog) {
        actionLogMapper.saveActionLog(actionLog);
        return actionLog.getActionLogId();
    }

    public List<ActionLogDto> getActionLogsByUserId(Long userId) {
        return actionLogMapper.getActionLogsByUserId(userId);
    }

    public boolean existsCompletedGame(Long userId) {
        return actionLogMapper.existsCompletedGame(userId);
    }

    public void lockGameUser(Long userId) {
        if (actionLogMapper.lockUserById(userId) == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
    }

    public int deleteActionLogsByUserId(Long userId) {
        return actionLogMapper.deleteActionLogsByUserId(userId);
    }
}
