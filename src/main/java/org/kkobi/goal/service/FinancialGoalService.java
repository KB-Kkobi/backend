package org.kkobi.goal.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.goal.domain.FinancialGoal;
import org.kkobi.goal.dto.request.FinancialGoalSaveRequestDto;
import org.kkobi.goal.dto.response.FinancialGoalRecommendationResponseDto;
import org.kkobi.goal.dto.response.FinancialGoalResponseDto;
import org.kkobi.goal.enums.FinancialGoalType;
import org.kkobi.goal.mapper.FinancialGoalMapper;
import org.kkobi.product.deposit.service.DepositProductService;
import org.kkobi.product.dto.request.ProductListRequestDto;
import org.kkobi.product.dto.response.ProductListResponseDto;
import org.kkobi.product.mapper.ProductMapper;
import org.kkobi.product.saving.service.SavingProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FinancialGoalService {

    private final FinancialGoalMapper financialGoalMapper;
    private final ProductMapper productMapper;
    private final DepositProductService depositProductService;
    private final SavingProductService savingProductService;
    private final FinancialGoalPlanner financialGoalPlanner;

    @Transactional(readOnly = true)
    public FinancialGoalResponseDto getGoal(Long userId) {
        FinancialGoal goal = financialGoalMapper.findByUserId(userId);
        return goal == null ? null : createResponse(goal);
    }

    @Transactional
    public FinancialGoalResponseDto saveGoal(
            Long userId,
            FinancialGoalSaveRequestDto request
    ) {
        FinancialGoal financialGoal = createFinancialGoal(userId, request);
        financialGoalMapper.upsert(financialGoal);

        FinancialGoal savedGoal = financialGoalMapper.findByUserId(userId);
        if (savedGoal == null) {
            throw new IllegalStateException("목표를 저장하지 못했습니다.");
        }
        return createResponse(savedGoal);
    }

    @Transactional
    public void deleteGoal(Long userId) {
        financialGoalMapper.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public FinancialGoalRecommendationResponseDto getRecommendations(
            Long userId,
            String productType,
            Integer page,
            Integer size
    ) {
        FinancialGoal goal = financialGoalMapper.findByUserId(userId);
        if (goal == null) {
            throw new IllegalArgumentException("먼저 목표를 설정해 주세요.");
        }

        String normalizedProductType = normalizeProductType(productType);
        List<Integer> availableTerms = productMapper.getAvailableSavingTerms(
                normalizedProductType
        );
        FinancialGoalPlan plan = financialGoalPlanner.createPlan(goal, availableTerms);
        ProductListRequestDto productRequest = createProductRequest(
                plan,
                page,
                size
        );
        ProductListResponseDto products = "DEPOSIT".equals(normalizedProductType)
                ? depositProductService.getDepositProductList(productRequest)
                : savingProductService.getSavingProductList(productRequest);

        return FinancialGoalRecommendationResponseDto.builder()
                .goal(createResponse(goal, plan))
                .products(products)
                .build();
    }

    private FinancialGoal createFinancialGoal(
            Long userId,
            FinancialGoalSaveRequestDto request
    ) {
        String customGoalName = normalizeCustomGoalName(
                request.getGoalType(),
                request.getCustomGoalName()
        );

        FinancialGoal financialGoal = new FinancialGoal();
        financialGoal.setUserId(userId);
        financialGoal.setGoalType(request.getGoalType());
        financialGoal.setCustomGoalName(customGoalName);
        financialGoal.setTargetAmount(request.getTargetAmount());
        financialGoal.setCurrentAmount(request.getCurrentAmount());
        financialGoal.setTargetMonths(request.getTargetMonths());
        return financialGoal;
    }

    private String normalizeCustomGoalName(
            FinancialGoalType goalType,
            String customGoalName
    ) {
        if (goalType != FinancialGoalType.OTHER) {
            return null;
        }
        String normalizedName = customGoalName == null ? "" : customGoalName.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("기타 목표명을 입력해 주세요.");
        }
        return normalizedName;
    }

    private FinancialGoalResponseDto createResponse(FinancialGoal goal) {
        FinancialGoalPlan plan = financialGoalPlanner.createPlan(
                goal,
                getAvailableTermsForGoal()
        );
        return createResponse(goal, plan);
    }

    private FinancialGoalResponseDto createResponse(
            FinancialGoal goal,
            FinancialGoalPlan plan
    ) {
        return FinancialGoalResponseDto.builder()
                .financialGoalId(goal.getFinancialGoalId())
                .goalType(goal.getGoalType())
                .customGoalName(goal.getCustomGoalName())
                .goalName(resolveGoalName(goal))
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .remainingAmount(plan.remainingAmount())
                .targetMonths(goal.getTargetMonths())
                .monthlyReferenceAmount(plan.monthlyReferenceAmount())
                .recommendedSavingTerm(plan.recommendedSavingTerm())
                .goalMatched(plan.goalMatched())
                .recommendationReason(plan.recommendationReason())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }

    private ProductListRequestDto createProductRequest(
            FinancialGoalPlan plan,
            Integer page,
            Integer size
    ) {
        ProductListRequestDto request = new ProductListRequestDto();
        request.setPage(page);
        request.setSize(size);
        request.setSort("maximumInterestRate,desc");
        request.setPreferredSavingTerm(
                plan.goalMatched() ? plan.recommendedSavingTerm() : null
        );
        return request;
    }

    private List<Integer> getAvailableTermsForGoal() {
        return Stream.concat(
                        safeTermStream(productMapper.getAvailableSavingTerms("DEPOSIT")),
                        safeTermStream(productMapper.getAvailableSavingTerms("SAVING"))
                )
                .filter(term -> term != null && term > 0)
                .distinct()
                .sorted()
                .toList();
    }

    private Stream<Integer> safeTermStream(List<Integer> terms) {
        return terms == null ? Stream.empty() : terms.stream();
    }

    private String normalizeProductType(String productType) {
        String normalized = productType == null
                ? ""
                : productType.trim().toUpperCase(Locale.ROOT);
        if (!"DEPOSIT".equals(normalized) && !"SAVING".equals(normalized)) {
            throw new IllegalArgumentException("상품 유형은 예금 또는 적금이어야 합니다.");
        }
        return normalized;
    }

    private String resolveGoalName(FinancialGoal goal) {
        return goal.getGoalType() == FinancialGoalType.OTHER
                ? goal.getCustomGoalName()
                : goal.getGoalType().getLabel();
    }
}
