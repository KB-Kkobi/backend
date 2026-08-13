package org.kkobi.securities.dto.request;

import lombok.Data;
import org.kkobi.securities.enums.SecurityType;

// 종목 목록 조회 요청
@Data
public class SecurityListRequest {

    // 종목 유형 필터 (없으면 전체)
    private SecurityType type;

    // 검색 키워드 (name 또는 ticker 부분 일치, 없으면 전체)
    private String keyword;

    // 페이지 번호 (1-based)
    private Integer page = 1;

    // 페이지당 종목 수
    private Integer size = 20;

    // 정렬 기준. 소문자 문자열로 받음 (match | change | volume | name). 기본값은 Service에서 처리
    private String sort;
}
