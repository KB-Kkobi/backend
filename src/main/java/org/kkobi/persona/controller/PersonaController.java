package org.kkobi.persona.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.persona.dto.PersonaResponseDto;
import org.kkobi.persona.service.PersonaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/personas")
@Tag(name = "투자 성향 유형", description = "투자 성향 유형(페르소나) 조회 API")
public class PersonaController {

    private final PersonaService personaService;

    @Operation(
            summary = "투자 성향 유형 전체 조회",
            description = "정의된 8개의 투자 성향 유형을 persona_id 오름차순으로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<PersonaResponseDto>> getAllPersonas() {
        return ResponseEntity.ok(personaService.getAllPersonas());
    }
}