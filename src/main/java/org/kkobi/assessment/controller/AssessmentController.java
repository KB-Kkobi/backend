package org.kkobi.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.dto.AssessmentResultResponseDto;
import org.kkobi.assessment.dto.LatestAssessmentResponse;
import org.kkobi.assessment.service.AssessmentResultService;
import org.kkobi.security.principal.CustomUserDetails;
import org.kkobi.users.dto.response.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentResultService assessmentResultService;

    @GetMapping("/me/latest")
    public ResponseEntity<?> getLatestAssessmentResult(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        AssessmentResultDetails resultDetails = assessmentResultService
                .getLatestAssessmentResultDetails(authenticatedUser.getUserId());
        if (resultDetails == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("성향 진단 이력이 없습니다."));
        }
        return ResponseEntity.ok(new LatestAssessmentResponse(resultDetails));
    }

    @Operation(
            summary = "개인 투자 성향 진단 결과 조회",
            description = "로그인한 사용자의 최신 투자 성향 진단 결과를 페르소나 정보와 함께 조회합니다."
    )
    @GetMapping("/me/result")
    public ResponseEntity<AssessmentResultResponseDto> getLatestAssessmentResultDto(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        AssessmentResultResponseDto result = assessmentResultService
                .getLatestAssessmentResult(authenticatedUser.getUserId());
        return ResponseEntity.ok(result);
    }
}
