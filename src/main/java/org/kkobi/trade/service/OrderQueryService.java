package org.kkobi.trade.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.trade.dto.OrderDto;
import org.kkobi.trade.dto.OrderSearchCondition;
import org.kkobi.trade.dto.TradeAccountDto;
import org.kkobi.trade.dto.request.OrderListRequest;
import org.kkobi.trade.dto.response.OrderListResponse;
import org.kkobi.trade.dto.response.OrderResponse;
import org.kkobi.trade.enums.OrderStatus;
import org.kkobi.trade.mapper.OrderMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final OrderMapper orderMapper;
    private final TradeAccountMapper accountMapper;

    @Transactional(readOnly = true)
    public OrderListResponse getOrders(Long userId, OrderListRequest req) {
        TradeAccountDto account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new IllegalStateException("계좌가 없습니다.");
        }

        if (req.getFrom() != null && req.getTo() != null && req.getFrom().isAfter(req.getTo())) {
            throw new IllegalArgumentException("from 날짜는 to 날짜보다 이후일 수 없습니다.");
        }

        int page = req.getPage() != null && req.getPage() >= 0 ? req.getPage() : 0;
        int size = req.getSize() != null && req.getSize() > 0 ? req.getSize() : DEFAULT_SIZE;

        String sort = "asc".equalsIgnoreCase(req.getSort()) ? "asc" : "desc";

        OrderSearchCondition condition = new OrderSearchCondition();
        condition.setAccountId(account.getAccountId());
        condition.setStatuses(parseStatuses(req.getStatus()));
        condition.setSecurityId(req.getSecurityId());
        condition.setFrom(req.getFrom());
        condition.setTo(req.getTo());
        condition.setSort(sort);
        condition.setOffset(page * size);
        condition.setSize(size);

        List<OrderDto> rows = orderMapper.search(condition);
        long totalElements = orderMapper.countSearch(condition);

        List<OrderResponse> orders = new ArrayList<>(rows.size());
        for (OrderDto row : rows) {
            orders.add(toOrderResponse(row));
        }

        return OrderListResponse.builder()
                .orders(orders)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .hasNext((long) (page + 1) * size < totalElements)
                .build();
    }

    private List<OrderStatus> parseStatuses(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return null;
        }
        List<OrderStatus> result = new ArrayList<>();
        for (String token : statusParam.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(OrderStatus.valueOf(trimmed));
            }
        }
        return result.isEmpty() ? null : result;
    }

    private OrderResponse toOrderResponse(OrderDto dto) {
        return OrderResponse.builder()
                .securityOrderId(dto.getSecurityOrderId())
                .securityId(dto.getSecurityId())
                .ticker(dto.getTicker())
                .name(dto.getName())
                .orderType(dto.getOrderType())
                .orderMethod(dto.getOrderMethod())
                .orderPrice(dto.getOrderPrice())
                .executedPrice(dto.getExecutedPrice())
                .quantity(dto.getQuantity())
                .executedAmount(dto.getExecutedAmount())
                .status(dto.getStatus())
                .orderedAt(toOffsetDateTime(dto.getOrderedAt()))
                .executedAt(toOffsetDateTime(dto.getExecutedAt()))
                .build();
    }

    private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ldt.atZone(KST).toOffsetDateTime();
    }
}
