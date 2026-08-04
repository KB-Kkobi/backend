package org.kkobi.product.dto.response;

import lombok.Data;

import java.util.List;

// 상품 상세 조회 응답 데이터
@Data
public class ProductDetailResponseDto {

    // 상품 식별자
    private Long productId;

    // 상품 유형
    private String productType;

    // 금융회사 이름
    private String financialCompanyName;

    // 상품 이름
    private String productName;

    // 가입 방법
    private String joinWay;

    // 만기 후 이자율 안내
    private String maturityInterestDescription;

    // 우대 조건
    private String preferentialConditions;

    // 가입 대상
    private String joinMember;

    // 기타 유의사항
    private String additionalNote;

    // 최고 가입 한도
    private Long maxLimit;

    // 상품 금리 옵션 목록
    private List<ProductOptionResponseDto> options;
}
