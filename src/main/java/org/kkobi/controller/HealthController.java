package org.kkobi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    // 애플리케이션 실행 상태를 반환
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkHeath() {
        return ResponseEntity.ok(Map.of("status", "up"));
    }
}
