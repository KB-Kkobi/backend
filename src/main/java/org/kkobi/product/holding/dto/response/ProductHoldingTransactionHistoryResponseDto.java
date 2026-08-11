package org.kkobi.product.holding.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductHoldingTransactionHistoryResponseDto {

    private Long productTransactionId;
    private Long holdingProductId;
    private String transactionType;
    private String productType;
    private String financialCompanyName;
    private String productName;
    private BigDecimal amount;
    private String status;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Seoul"
    )
    private LocalDateTime processedAt;
}
