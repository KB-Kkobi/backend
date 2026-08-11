package org.kkobi.trade.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class OrderListRequest {
    private String status;      // 콤마 구분 다중값 (예: "PENDING,FILLED")
    private Long securityId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
    private String sort;        // 정렬 방향: "asc" | "desc" (기본 "desc")
    private Integer page;
    private Integer size;
}
