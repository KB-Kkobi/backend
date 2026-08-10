package org.kkobi.securities.dto.response;

import lombok.Data;

import java.util.List;

// 종목 목록 조회 응답
@Data
public class SecurityListResponse {

    // 현재 페이지의 종목 목록
    private List<SecurityListItemResponse> content;

    // 현재 페이지 번호 (1-based)
    private Integer page;

    // 페이지당 종목 수
    private Integer size;

    // 전체 종목 수
    private long totalElements;

    // 전체 페이지 수
    private int totalPages;
}
