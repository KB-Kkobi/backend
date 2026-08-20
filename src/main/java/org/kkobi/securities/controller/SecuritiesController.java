package org.kkobi.securities.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.securities.dto.request.QuoteRequest;
import org.kkobi.securities.dto.request.SecurityListRequest;
import org.kkobi.securities.dto.response.QuoteResponse;
import org.kkobi.securities.dto.response.SecurityDetailResponse;
import org.kkobi.securities.dto.response.SecurityListResponse;
import org.kkobi.securities.dto.response.SecurityRecommendationResponse;
import org.kkobi.securities.service.SecurityQuoteService;
import org.kkobi.securities.service.SecurityService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/securities")
@RequiredArgsConstructor
public class SecuritiesController {

    private final SecurityService securityService;
    private final SecurityQuoteService securityQuoteService;

    // 종목 목록 조회
    @GetMapping
    public ResponseEntity<SecurityListResponse> getSecurityList(
            @ModelAttribute SecurityListRequest request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {

        return ResponseEntity.ok(
                securityService.getSecurityList(request, authenticatedUser.getUserId()));
    }

    // 홈 화면 추천 종목 조회 (주식·주식형 ETF 통합 1 + 채권형 ETF 1, 성향 매칭 순)
    @GetMapping("/recommendations")
    public ResponseEntity<SecurityRecommendationResponse> getRecommendedSecurities(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {

        return ResponseEntity.ok(
                securityService.getRecommendedSecurities(authenticatedUser.getUserId()));
    }

    // ticker로 종목 상세 조회
    @GetMapping("/{ticker}")
    public ResponseEntity<SecurityDetailResponse> getSecurityByTicker(
            @PathVariable String ticker) {

        return ResponseEntity.ok(securityService.getSecurityByTicker(ticker));
    }

    // 여러 ticker에 대한 실시간 시세(현재가/전일대비/등락률/전일종가) 배치 조회
    @PostMapping("/quotes")
    public ResponseEntity<QuoteResponse> getQuotes(@RequestBody QuoteRequest request) {
        return ResponseEntity.ok(securityQuoteService.getQuotes(request));
    }
}
