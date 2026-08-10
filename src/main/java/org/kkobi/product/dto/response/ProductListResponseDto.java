package org.kkobi.product.dto.response;

import lombok.Data;

import java.util.List;

// 상품 목록 조회 응답 데이터
@Data
public class ProductListResponseDto {

    // 현재 페이지의 상품 목록
    private List<ProductListItemResponseDto> content;

    // 현재 페이지 번호
    private Integer page;

    // 페이지당 상품 수
    private Integer size;

    // 전체 상품 수
    private Long totalElements;

    // 전체 페이지 수
    private Integer totalPages;
}
