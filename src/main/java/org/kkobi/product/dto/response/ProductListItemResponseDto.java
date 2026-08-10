package org.kkobi.product.dto.response;

import lombok.Data;

import java.math.BigDecimal;

// 상품 목록 항목 조회 응답 데이터
@Data
public class ProductListItemResponseDto {

    // 상품 식별자
    private Long productId;

    // 상품 유형
    private String productType;

    // 금융회사 이름
    private String financialCompanyName;

    // 상품 이름
    private String productName;

    // 가입 기간
    private Integer savingTerm;

    // 기본 금리
    private BigDecimal interestRate;

    // 최고 우대 금리
    private BigDecimal maximumInterestRate;

    // 최고 가입 한도
    private Long maxLimit;

    // 가입 방법
    private String joinway;

    // 적립 유형 코드
    private String reserveType;

    // 적립 유형 이름
    private String reserveTypeName;
}
