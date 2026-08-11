package org.kkobi.trade.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderListRequest {
    private String status;      // 콤마 구분 다중값 (예: "PENDING,FILLED")
    private Long securityId;
    private LocalDate from;
    private LocalDate to;
    private Integer page;
    private Integer size;
}
