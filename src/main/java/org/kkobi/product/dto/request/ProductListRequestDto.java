package org.kkobi.product.dto.request;

import lombok.Data;

// 상품 목록 조회 요청 데이터
@Data
public class ProductListRequestDto {

    // 금융회사명 또는 상품명 검색어
    private String keyword;

    // 가입 기간
    private Integer savingTerm;

    // 적립 유형 코드
    private String reserveType;

    // 페이지 번호
    private Integer page = 1;

    // 페이지당 상품 수
    private Integer size = 5;

    // 정렬 조건
    private String sort = "maximumInterestRate,desc";
}
