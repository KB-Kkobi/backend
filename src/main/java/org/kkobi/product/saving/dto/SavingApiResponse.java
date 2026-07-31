package org.kkobi.product.saving.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SavingApiResponse {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public  static class Result {

        // API 응답 코드
        @JsonProperty("err_cd")
        private String errCd;

        // API 응답 메시지
        @JsonProperty("err_msg")
        private String errMsg;

        // 전체 상품 개수
        @JsonProperty("total_count")
        private int totalCount;

        // 전체 페이지 수
        @JsonProperty("max_page_no")
        private int maxPageNo;

        // 현재 페이지 번호
        @JsonProperty("now_page_no")
        private int nowPageNo;

        // 적금 상품 기본 정보 목록
        private List<SavingProductDto> baseList;

        // 적금 상품 금리 옵션 목록
        private List<SavingProductOptionDto> optionList;
        
    }
}
