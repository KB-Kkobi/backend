package org.kkobi.securities.dto.response;

import lombok.Data;

import java.util.List;

// 홈 화면 추천 종목 응답 (주식 1 + 주식형 ETF 1 + 채권형 ETF 1)
@Data
public class SecurityRecommendationResponse {

    // 추천 종목 목록
    private List<SecurityListItemResponse> content;

    // 성향 진단 없이 match 정렬 요청 시 volume으로 대체된 경우 true
    private boolean sortFallback;

    // 실제 적용된 정렬 키 (소문자: "match" | "volume")
    private String appliedSort;
}
