package org.kkobi.product.deposit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DepositProductOptionDto {

    // 어떤 금융회사의 상품인지 식별
    @JsonProperty("fin_co_no")
    private String financialCompanyNumber;

    // 어떤 상품의 옵션인지 식별
    @JsonProperty("fin_prdt_cd")
    private String productCode;

    // 저축 금리 유형 코드
    @JsonProperty("intr_rate_type")
    private String interestRateType;

    // 저축 금리 유형명
    @JsonProperty("intr_rate_type_nm")
    private String interestRateTypeName;

    // 저축 기간 (개월)
    @JsonProperty("save_trm")
    private String savingTerm;

    // 기본 금리
    @JsonProperty("intr_rate")
    private BigDecimal interestRate;

    // 최고 우대 금리
    @JsonProperty("intr_rate2")
    private BigDecimal maximumInterestRate;
}
