package org.kkobi.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.dto.LatestAssessmentResponse;
import org.kkobi.assessment.service.AssessmentResultService;
import org.kkobi.security.principal.CustomUserDetails;
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
    public ResponseEntity<LatestAssessmentResponse> getLatestAssessmentResult(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {
        AssessmentResultDetails resultDetails = assessmentResultService
                .getLatestAssessmentResultDetails(authenticatedUser.getUserId());
        return ResponseEntity.ok(new LatestAssessmentResponse(resultDetails));
    }
}
