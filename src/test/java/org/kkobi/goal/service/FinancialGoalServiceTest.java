package org.kkobi.goal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.goal.domain.FinancialGoal;
import org.kkobi.goal.dto.response.FinancialGoalRecommendationResponseDto;
import org.kkobi.goal.enums.FinancialGoalType;
import org.kkobi.goal.mapper.FinancialGoalMapper;
import org.kkobi.product.deposit.service.DepositProductService;
import org.kkobi.product.dto.request.ProductListRequestDto;
import org.kkobi.product.dto.response.ProductListResponseDto;
import org.kkobi.product.mapper.ProductMapper;
import org.kkobi.product.saving.service.SavingProductService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialGoalServiceTest {

    @Mock
    private FinancialGoalMapper financialGoalMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private DepositProductService depositProductService;
    @Mock
    private SavingProductService savingProductService;

    private FinancialGoalService service;

    @BeforeEach
    void setUp() {
        service = new FinancialGoalService(
                financialGoalMapper,
                productMapper,
                depositProductService,
                savingProductService,
                new FinancialGoalPlanner()
        );
    }

    @Test
    void requestedSavingTypeUsesActualTermAndExistingListSort() {
        FinancialGoal goal = createGoal(600_000L, 12);
        ProductListResponseDto productList = new ProductListResponseDto();
        productList.setContent(List.of());

        when(financialGoalMapper.findByUserId(7L)).thenReturn(goal);
        when(productMapper.getAvailableSavingTerms("SAVING"))
                .thenReturn(List.of(6, 12, 24));
        when(savingProductService.getSavingProductList(
                org.mockito.ArgumentMatchers.any(ProductListRequestDto.class)
        )).thenReturn(productList);

        FinancialGoalRecommendationResponseDto response =
                service.getRecommendations(7L, "SAVING", 1, 5);

        ArgumentCaptor<ProductListRequestDto> captor =
                ArgumentCaptor.forClass(ProductListRequestDto.class);
        verify(savingProductService).getSavingProductList(captor.capture());
        verify(depositProductService, never())
                .getDepositProductList(org.mockito.ArgumentMatchers.any());

        assertEquals(null, captor.getValue().getSavingTerms());
        assertEquals(12, captor.getValue().getPreferredSavingTerm());
        assertEquals("maximumInterestRate,desc", captor.getValue().getSort());
        assertEquals(2_400_000L, response.getGoal().getRemainingAmount());
        assertEquals(200_000L, response.getGoal().getMonthlyReferenceAmount());
        assertTrue(response.getGoal().isGoalMatched());
        assertEquals(productList, response.getProducts());
    }

    @Test
    void requestedDepositTypeIsNotForcedToSaving() {
        FinancialGoal goal = createGoal(0L, 12);
        ProductListResponseDto productList = new ProductListResponseDto();
        productList.setContent(List.of());

        when(financialGoalMapper.findByUserId(7L)).thenReturn(goal);
        when(productMapper.getAvailableSavingTerms("DEPOSIT"))
                .thenReturn(List.of(6, 12, 24));
        when(depositProductService.getDepositProductList(
                org.mockito.ArgumentMatchers.any(ProductListRequestDto.class)
        )).thenReturn(productList);

        FinancialGoalRecommendationResponseDto response =
                service.getRecommendations(7L, "DEPOSIT", 1, 5);

        verify(depositProductService)
                .getDepositProductList(org.mockito.ArgumentMatchers.any());
        verify(savingProductService, never())
                .getSavingProductList(org.mockito.ArgumentMatchers.any());
        assertEquals(productList, response.getProducts());
    }

    @Test
    void rejectsUnsupportedProductType() {
        when(financialGoalMapper.findByUserId(7L))
                .thenReturn(createGoal(0L, 12));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getRecommendations(7L, "FUND", 1, 5)
        );
    }

    @Test
    void deletesGoalForCurrentUser() {
        service.deleteGoal(7L);

        verify(financialGoalMapper).deleteByUserId(7L);
    }

    private FinancialGoal createGoal(long currentAmount, int targetMonths) {
        FinancialGoal goal = new FinancialGoal();
        goal.setFinancialGoalId(3L);
        goal.setUserId(7L);
        goal.setGoalType(FinancialGoalType.TRAVEL);
        goal.setTargetAmount(3_000_000L);
        goal.setCurrentAmount(currentAmount);
        goal.setTargetMonths(targetMonths);
        return goal;
    }
}
