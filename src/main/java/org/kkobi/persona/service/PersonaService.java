package org.kkobi.persona.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.persona.dto.PersonaResponseDto;
import org.kkobi.persona.mapper.PersonaMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaMapper personaMapper;

    // 투자 성향 유형 전체 목록 조회
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> getAllPersonas() {
        return personaMapper.findAllPersonas();
    }
}