package org.kkobi.securities.dto.response;

import java.util.List;

// 다건 시세 조회 응답
public record QuoteResponse(List<QuoteItem> quotes) {
}
