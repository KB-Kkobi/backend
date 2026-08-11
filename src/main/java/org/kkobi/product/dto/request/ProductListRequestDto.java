package org.kkobi.product.dto.request;

import lombok.Data;
import org.kkobi.product.enums.PreferentialConditionType;

import java.util.List;

// 상품 목록 조회 요청 데이터
@Data
public class ProductListRequestDto {

    // 금융회사명 또는 상품명 검색어
    private String keyword;

    // 가입 기간
    private Integer savingTerm;

    // 복수 가입 기간
    private List<Integer> savingTerms;

    // 우대조건 유형
    private List<PreferentialConditionType> preferentialConditions;

    // 적립 유형 코드
    private String reserveType;

    // 복수 적립 유형 코드
    private List<String> reserveTypes;

    // 페이지 번호
    private Integer page = 1;

    // 페이지당 상품 수
    private Integer size = 5;

    // 정렬 조건
    private String sort = "maximumInterestRate,desc";
}
