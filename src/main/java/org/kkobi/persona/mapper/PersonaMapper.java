package org.kkobi.persona.mapper;

import org.kkobi.persona.dto.PersonaResponseDto;

import java.util.List;

public interface PersonaMapper {

    List<PersonaResponseDto> findAllPersonas();
}