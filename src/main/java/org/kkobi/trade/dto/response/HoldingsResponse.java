package org.kkobi.trade.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class HoldingsResponse {
    private List<HoldingItemResponse> holdings;
    private OffsetDateTime quotedAt;
}
