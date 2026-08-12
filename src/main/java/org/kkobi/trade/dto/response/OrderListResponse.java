package org.kkobi.trade.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderListResponse {
    private List<OrderResponse> orders;
    private int page;
    private int size;
    private long totalElements;
    private boolean hasNext;
}
