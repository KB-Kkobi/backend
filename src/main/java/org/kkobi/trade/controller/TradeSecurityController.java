package org.kkobi.trade.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.common.dto.ApiResponse;
import org.kkobi.security.principal.CustomUserDetails;
import org.kkobi.trade.dto.response.OrderableResponse;
import org.kkobi.trade.dto.response.SecurityQuoteResponse;
import org.kkobi.trade.service.TradeSecurityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/securities")
@RequiredArgsConstructor
public class TradeSecurityController {

    private final TradeSecurityService tradeSecurityService;

    @GetMapping("/{securityId}/quote")
    public ResponseEntity<ApiResponse<SecurityQuoteResponse>> getQuote(
            @PathVariable Long securityId) {

        SecurityQuoteResponse data = tradeSecurityService.getQuote(securityId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{securityId}/orderable")
    public ResponseEntity<ApiResponse<OrderableResponse>> getOrderable(
            @PathVariable Long securityId,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {

        OrderableResponse data = tradeSecurityService.getOrderable(securityId, authenticatedUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
