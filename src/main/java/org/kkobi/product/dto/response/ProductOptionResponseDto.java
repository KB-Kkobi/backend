package org.kkobi.product.dto.response;


import lombok.Data;

import java.math.BigDecimal;

// 상품 금리 옵션 조회 응답 데이터
@Data
public class ProductOptionResponseDto {

    // 상품 옵션 식별자
    private Long productOptionId;

    // 금리 유형 코드
    private String interestRateType;

    // 금리 유형 이름
    private String interestRateTypeName;

    // 적립 유형 코드, 예금은 NONE
    private String reserveType;

    // 적립 유형 이름
    private String reserveTypeName;

    // 가입 기간
    private Integer savingTerm;

    // 기본 금리
    private Integer interestRate;

    // 최고 우대 금리
    private BigDecimal maximumInterestRate;
}
