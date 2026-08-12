package org.kkobi.trade.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.common.dto.ApiResponse;
import org.kkobi.security.principal.CustomUserDetails;
import org.kkobi.trade.dto.response.HoldingsResponse;
import org.kkobi.trade.dto.response.PortfolioResponse;
import org.kkobi.trade.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/me")
@RequiredArgsConstructor
public class TradeAccountController {

    private final PortfolioService portfolioService;

    @GetMapping("/portfolio")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {

        PortfolioResponse data = portfolioService.getPortfolio(authenticatedUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/holdings")
    public ResponseEntity<ApiResponse<HoldingsResponse>> getHoldings(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {

        HoldingsResponse data = portfolioService.getHoldings(authenticatedUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
