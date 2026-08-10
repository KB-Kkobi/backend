package org.kkobi.securities.dto.request;

import lombok.Data;

import java.util.List;

// 다건 시세 조회 요청
@Data
public class QuoteRequest {

    // 시세를 조회할 ticker 목록
    private List<String> tickers;
}
