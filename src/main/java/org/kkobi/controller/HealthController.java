package org.kkobi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(
        name = "서버 상태 확인 API",
        description = "백엔드 서버의 정상 동작 여부를 확인하는 API"
)
public class HealthController {

    // 애플리케이션 실행 상태를 반환
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkHeath() {
        return ResponseEntity.ok(Map.of("status", "up"));
    }
}
