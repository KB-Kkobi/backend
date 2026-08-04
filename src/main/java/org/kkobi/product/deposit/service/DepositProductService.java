package org.kkobi.product.deposit.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.product.deposit.dto.DepositApiResponse;
import org.kkobi.product.deposit.dto.DepositProductDto;
import org.kkobi.product.deposit.dto.DepositProductOptionDto;
import org.kkobi.product.dto.response.ProductDetailResponseDto;
import org.kkobi.product.dto.response.ProductOptionResponseDto;
import org.kkobi.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

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

    // 예금 상품 유형 코드
    private static final String DEPOSIT_PRODUCT_TYPE = "DEPOSIT";

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
    @Transactional
    public void collectDepositProducts() {

        DepositApiResponse response = getDepositProducts(1);
        DepositApiResponse.Result result = validateDepositApiResponse(response);

        saveDepositProducts(result);

        int maxPageNo = result.getMaxPageNo();

        for(int pageNumber= 2; pageNumber <= maxPageNo; pageNumber++){
            response = getDepositProducts(pageNumber);
            result = validateDepositApiResponse(response);

            saveDepositProducts(result);
        }
    }

    // 상품 ID로 예금 상품 상세 정보와 금리 옵션을 조회
    @Transactional(readOnly = true)
    public ProductDetailResponseDto getDepostProductDetail(Long productId){

        // 예금 상품 기본 정보 조회
        ProductDetailResponseDto productDetail =
                productMapper.getProductDetail(
                        productId,
                        DEPOSIT_PRODUCT_TYPE
                );

        // 예금 상품이 존재하지 않으면 예외 발생
        if(productDetail == null){
            throw new IllegalArgumentException(
                    "존재하지 않는 예금 상품입니다."
            );
        }

        // 예금 상품의 금리 옵션 목록 조회
        List<ProductOptionResponseDto> options =
                productMapper.getProductOptions(productId);

        // 상품 기본 정보에 금리 옵션 목록 설정
        productDetail.setOptions(options);

        // 예금 상품 상세 정보 반환
        return productDetail;
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

    // 금융감독원 API 응답이 정상인지 확인
    private DepositApiResponse.Result validateDepositApiResponse(
            DepositApiResponse response){

        if(response == null || response.getResult() == null){
            throw new IllegalStateException("금융감독원 예금 상품 API 응답이 없습니다.");
        }

        DepositApiResponse.Result result = response.getResult();

        if(!"000".equals(result.getErrCd())){
            throw new IllegalStateException(
                    "금융감독원 예금 상품 API 호출에 실패했습니다. "
                    + "오류 코드: " + result.getErrCd()
                    + ", 오류 메시지: " + result.getErrMsg()
            );
        }

        if(result.getBaseList() == null || result.getOptionList() == null){
            throw new IllegalStateException("금융감독원 예금 상품 데이터가 없습니다.");
        }

        return result;
    }
}
