package org.kkobi.trade.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.common.dto.ApiResponse;
import org.kkobi.security.principal.CustomUserDetails;
import org.kkobi.trade.dto.CancelOrderResult;
import org.kkobi.trade.dto.PlaceOrderRequest;
import org.kkobi.trade.dto.PlaceOrderResult;
import org.kkobi.trade.dto.request.CreateOrderRequest;
import org.kkobi.trade.dto.request.OrderListRequest;
import org.kkobi.trade.dto.response.CancelOrderResponse;
import org.kkobi.trade.dto.response.OrderListResponse;
import org.kkobi.trade.dto.response.OrderResponse;
import org.kkobi.trade.service.OrderQueryService;
import org.kkobi.trade.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class TradeOrderController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {

        PlaceOrderRequest serviceRequest = toPlaceOrderRequest(request);
        PlaceOrderResult result = orderService.placeOrder(authenticatedUser.getUserId(), serviceRequest);
        OrderResponse orderResponse = toOrderResponse(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(orderResponse));
    }

    @DeleteMapping("/{securityOrderId}")
    public ResponseEntity<ApiResponse<CancelOrderResponse>> cancelOrder(
            @PathVariable Long securityOrderId,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {

        CancelOrderResult result = orderService.cancelOrder(authenticatedUser.getUserId(), securityOrderId);
        CancelOrderResponse cancelResponse = toCancelOrderResponse(result);
        return ResponseEntity.ok(ApiResponse.success(cancelResponse));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            @ModelAttribute OrderListRequest request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser) {

        OrderListResponse data = orderQueryService.getOrders(authenticatedUser.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private PlaceOrderRequest toPlaceOrderRequest(CreateOrderRequest req) {
        PlaceOrderRequest r = new PlaceOrderRequest();
        r.setSecurityId(req.getSecurityId());
        r.setOrderType(req.getOrderType());
        r.setOrderMethod(req.getOrderMethod());
        r.setPrice(req.getPrice());
        r.setQuantity(req.getQuantity());
        return r;
    }

    private OrderResponse toOrderResponse(PlaceOrderResult result) {
        return OrderResponse.builder()
                .securityOrderId(result.getSecurityOrderId())
                .securityId(result.getSecurityId())
                .ticker(result.getTicker())
                .name(result.getName())
                .orderType(result.getOrderType())
                .orderMethod(result.getOrderMethod())
                .orderPrice(result.getOrderPrice())
                .executedPrice(result.getExecutedPrice())
                .quantity(result.getQuantity())
                .executedAmount(result.getExecutedAmount())
                .status(result.getStatus())
                .orderedAt(toOffsetDateTime(result.getOrderedAt()))
                .executedAt(toOffsetDateTime(result.getExecutedAt()))
                .build();
    }

    private CancelOrderResponse toCancelOrderResponse(CancelOrderResult result) {
        return CancelOrderResponse.builder()
                .securityOrderId(result.getSecurityOrderId())
                .status(result.getStatus())
                .updatedAt(toOffsetDateTime(result.getUpdatedAt()))
                .build();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ldt.atZone(KST).toOffsetDateTime();
    }
}
