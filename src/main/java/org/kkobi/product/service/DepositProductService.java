package org.kkobi.product.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.product.dto.DepositApiResponse;
import org.kkobi.product.dto.DepositProductDto;
import org.kkobi.product.dto.DepositProductOptionDto;
import org.kkobi.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class DepositProductService {

    // 외부 API 호출에 사용하는 HTTP 클라이언트
    private final RestTemplate restTemplate;
    // 예금 상품 DB 접근 Mapper
    private final ProductMapper productMapper;

    // 금융감독원 금융상품통합비교공시 API 기본 주소
    @Value("${finlife.api.base-url}")
    private String apiBaseUrl;

    // 금융감독원 Open API 인증키
    @Value("${finlife.api.key}")
    private String apiKey;

    // 은행권 금융회사 그룹 코드
    private static final String BANK_GROUP_CODE = "020000";

    // 금융감독원 API에서 예금 상품 정보를 조회
    public DepositApiResponse getDepositProducts(int pageNumber){

        URI uri = UriComponentsBuilder
                .fromHttpUrl(apiBaseUrl)
                .path("/depositProductsSearch.json")
                .queryParam("auth", apiKey)
                .queryParam("topFinGrpNo", BANK_GROUP_CODE)
                .queryParam("pageNo", pageNumber)
                .build()
                .encode()
                .toUri();

        DepositApiResponse response =
                restTemplate.getForObject(uri, DepositApiResponse.class);

        return response;
    }


    // 금감원에서 예금 상품 데이터를 조회한 후 DB에 저장
    public void collectDepositProducts() {

        DepositApiResponse response = getDepositProducts(1);
        DepositApiResponse.Result result = response.getResult();

        saveDepositProducts(result);

        int maxPageNo = result.getMaxPageNo();

        for(int pageNumber= 2; pageNumber <= maxPageNo; pageNumber++){
            response = getDepositProducts(pageNumber);
            result = response.getResult();

            saveDepositProducts(result);
        }
    }

    // 금감원에서 조회한 한 페이지의 예금 상품 및 옵션을 DB에 저장
    private void saveDepositProducts(DepositApiResponse.Result result){

        for(DepositProductDto product : result.getBaseList()) {
            productMapper.saveDepositProduct(product);
        }

        for(DepositProductOptionDto option : result.getOptionList()){
            Long productId = productMapper.getDepositProductId(
                    option.getFinancialCompanyNumber(),
                    option.getProductCode()
            );

            productMapper.saveDepositProductOption(productId, option);
        }
    }
}
