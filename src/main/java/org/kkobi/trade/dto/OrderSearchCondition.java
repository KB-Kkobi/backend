package org.kkobi.trade.dto;

import lombok.Data;
import org.kkobi.trade.enums.OrderStatus;

import java.time.LocalDate;
import java.util.List;

@Data
public class OrderSearchCondition {
    private Long accountId;
    private List<OrderStatus> statuses;
    private Long securityId;
    private LocalDate from;
    private LocalDate to;
    private int offset;
    private int size;
}
