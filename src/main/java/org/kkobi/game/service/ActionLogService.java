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
}
