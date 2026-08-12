package org.kkobi.trade.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kkobi.trade.dto.OrderDto;
import org.kkobi.trade.dto.OrderSearchCondition;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    int insert(OrderDto order);

    OrderDto findById(@Param("securityOrderId") Long securityOrderId);

    int updateFilled(
            @Param("securityOrderId") Long securityOrderId,
            @Param("executedPrice") Long executedPrice,
            @Param("executedAt") LocalDateTime executedAt);

    int updateCancelled(@Param("securityOrderId") Long securityOrderId);

    List<OrderDto> search(OrderSearchCondition condition);

    long countSearch(OrderSearchCondition condition);

    // idx_orders_pending_security (status, security_id) 인덱스 활용
    List<OrderDto> findPendingLimitBySecurityId(@Param("securityId") Long securityId);

    // 중복 체결 방지: WHERE security_order_id = ? AND status = 'PENDING'
    int updateFilledConditional(
            @Param("securityOrderId") Long securityOrderId,
            @Param("executedPrice") Long executedPrice,
            @Param("executedAt") LocalDateTime executedAt);

    // 장마감: 당일 PENDING 주문 전체 EXPIRED 처리
    int updateExpiredBatch();

    // 장마감 잠금 해제를 위해 당일 PENDING 주문 목록 선조회
    List<OrderDto> findTodayPending();
}
