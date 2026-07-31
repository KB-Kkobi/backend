package org.kkobi.product.saving.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SavingProductDto {

    // 금융회사 식별 번호
    @JsonProperty("fin_co_no")
    private String financialCompanyNumber;

    // 금융상품 식별 코드
    @JsonProperty("fin_prdt_cd")
    private String productCode;

    // 금융회사명
    @JsonProperty("kor_co_nm")
    private String financialCompanyName;

    // 금융상품명
    @JsonProperty("fin_prdt_nm")
    private String productName;

    // 가입 방법
    @JsonProperty("join_way")
    private String joinWay;

    // 만기 후 이자율 안내
    @JsonProperty("mtrt_int")
    private String maturityInterestDescription;

    // 우대 조건
    @JsonProperty("spcl_cnd")
    private String preferentialConditions;

    // 가입 대상
    @JsonProperty("join_member")
    private String joinMember;

    // 기타 유의사항
    @JsonProperty("etc_note")
    private String additionalNote;

    // 최고 한도
    @JsonProperty("max_limit")
    private Long maxLimit;
}
