package org.kkobi.securities.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.service.AssessmentResultService;
import org.kkobi.securities.dto.response.SecurityListItemResponse;
import org.kkobi.securities.dto.response.SecurityRecommendationResponse;
import org.kkobi.securities.enums.SecurityType;
import org.kkobi.securities.enums.StockSortType;
import org.kkobi.securities.mapper.SecurityMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityServiceTest {

    @Test
    @DisplayName("성향 진단이 있으면 주식·주식형ETF 통합 1개 + 채권형ETF 1개를 매칭 순으로 추천한다")
    void recommendsStockGroupAndBondEtfByMatch() {
        SecurityMapper securityMapper = mock(SecurityMapper.class);
        AssessmentResultService assessmentResultService = mock(AssessmentResultService.class);

        when(assessmentResultService.existsAssessmentResult(1L)).thenReturn(true);
        when(assessmentResultService.getLatestAssessmentScore(1L))
                .thenReturn(new AssessmentScore(
                        BigDecimal.valueOf(50), BigDecimal.valueOf(50), BigDecimal.valueOf(50)));

        SecurityListItemResponse topEquityEtf = securityItem(SecurityType.EQUITY_ETF, "90.0");
        SecurityListItemResponse topBondEtf = securityItem(SecurityType.BOND_ETF, "80.0");

        when(securityMapper.getSecurityList(
                eq(List.of(SecurityType.STOCK, SecurityType.EQUITY_ETF)),
                isNull(), eq(0), eq(1), eq(StockSortType.MATCH), any(), any(), any()))
                .thenReturn(List.of(topEquityEtf));

        when(securityMapper.getSecurityList(
                eq(List.of(SecurityType.BOND_ETF)),
                isNull(), eq(0), eq(1), eq(StockSortType.MATCH), any(), any(), any()))
                .thenReturn(List.of(topBondEtf));

        SecurityService service = new SecurityService(securityMapper, assessmentResultService);

        SecurityRecommendationResponse response = service.getRecommendedSecurities(1L);

        assertFalse(response.isSortFallback());
        assertEquals("match", response.getAppliedSort());
        assertEquals(2, response.getContent().size());
        assertEquals(SecurityType.EQUITY_ETF, response.getContent().get(0).getType());
        assertEquals(SecurityType.BOND_ETF, response.getContent().get(1).getType());
    }

    @Test
    @DisplayName("성향 진단이 없으면 거래량 기준으로 대체하고 sortFallback을 true로 반환한다")
    void fallsBackToVolumeSortWithoutAssessment() {
        SecurityMapper securityMapper = mock(SecurityMapper.class);
        AssessmentResultService assessmentResultService = mock(AssessmentResultService.class);

        when(assessmentResultService.existsAssessmentResult(1L)).thenReturn(false);

        when(securityMapper.getSecurityList(
                eq(List.of(SecurityType.STOCK, SecurityType.EQUITY_ETF)),
                isNull(), eq(0), eq(1), eq(StockSortType.VOLUME), isNull(), isNull(), isNull()))
                .thenReturn(List.of(securityItem(SecurityType.STOCK, null)));

        when(securityMapper.getSecurityList(
                eq(List.of(SecurityType.BOND_ETF)),
                isNull(), eq(0), eq(1), eq(StockSortType.VOLUME), isNull(), isNull(), isNull()))
                .thenReturn(List.of(securityItem(SecurityType.BOND_ETF, null)));

        SecurityService service = new SecurityService(securityMapper, assessmentResultService);

        SecurityRecommendationResponse response = service.getRecommendedSecurities(1L);

        assertTrue(response.isSortFallback());
        assertEquals("volume", response.getAppliedSort());
        assertEquals(2, response.getContent().size());
    }

    private SecurityListItemResponse securityItem(SecurityType type, String matchScore) {
        SecurityListItemResponse item = new SecurityListItemResponse();
        item.setType(type);
        item.setMatchScore(matchScore == null ? null : new BigDecimal(matchScore));
        return item;
    }
}
