package org.kkobi.product.holding.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SavingsAssetStatusResponseDto {

    // 전체 저축 원금
    private BigDecimal totalPrincipal;

    // 전체 세전 발생 이자
    private BigDecimal totalAccruedInterest;

    // 전체 세전 평가금액
    private BigDecimal totalCurrentValue;

    // 전체 세후 이자
    private BigDecimal totalAfterTaxInterest;

    // 전체 세후 평가금액
    private BigDecimal totalAfterTaxCurrentValue;

    // 전체 저축 자산 수익률
    private BigDecimal totalReturnRate;

    // 보유 예적금 목록
    private List<ProductHoldingListItemResponseDto> holdings;
}
