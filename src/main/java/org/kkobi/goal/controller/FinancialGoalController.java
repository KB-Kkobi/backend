package org.kkobi.goal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.goal.dto.request.FinancialGoalSaveRequestDto;
import org.kkobi.goal.dto.response.FinancialGoalRecommendationResponseDto;
import org.kkobi.goal.dto.response.FinancialGoalResponseDto;
import org.kkobi.goal.service.FinancialGoalService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial-goals")
@Tag(name = "목표 설정", description = "회원별 목표 설정과 맞춤 상품 추천 API")
public class FinancialGoalController {

    private final FinancialGoalService financialGoalService;

    @Operation(summary = "저장한 목표 조회")
    @GetMapping
    public ResponseEntity<FinancialGoalResponseDto> getGoal(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        FinancialGoalResponseDto goal = financialGoalService.getGoal(
                authenticatedUser.getUserId()
        );
        return goal == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(goal);
    }

    @Operation(summary = "목표 생성 또는 수정")
    @PutMapping
    public ResponseEntity<FinancialGoalResponseDto> saveGoal(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @Valid @RequestBody FinancialGoalSaveRequestDto request
    ) {
        return ResponseEntity.ok(financialGoalService.saveGoal(
                authenticatedUser.getUserId(),
                request
        ));
    }

    @Operation(summary = "저장한 목표 삭제")
    @DeleteMapping
    public ResponseEntity<Void> deleteGoal(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        financialGoalService.deleteGoal(authenticatedUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "목표 맞춤 예금·적금 추천")
    @GetMapping("/recommendations")
    public ResponseEntity<FinancialGoalRecommendationResponseDto> getRecommendations(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @RequestParam(defaultValue = "DEPOSIT") String productType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer size
    ) {
        return ResponseEntity.ok(financialGoalService.getRecommendations(
                authenticatedUser.getUserId(),
                productType,
                page,
                size
        ));
    }
}
