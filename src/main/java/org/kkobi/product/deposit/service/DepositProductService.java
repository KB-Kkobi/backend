package org.kkobi.product.deposit.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.product.deposit.dto.DepositApiResponse;
import org.kkobi.product.deposit.dto.DepositProductDto;
import org.kkobi.product.deposit.dto.DepositProductOptionDto;
import org.kkobi.product.dto.request.ProductListRequestDto;
import org.kkobi.product.dto.response.ProductDetailResponseDto;
import org.kkobi.product.dto.response.ProductListItemResponseDto;
import org.kkobi.product.dto.response.ProductListResponseDto;
import org.kkobi.product.dto.response.ProductOptionResponseDto;
import org.kkobi.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.kkobi.product.service.ProductPreferentialConditionService;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepositProductService {

    // 외부 API 호출에 사용하는 HTTP 클라이언트
    private final RestTemplate restTemplate;

    // 예금 상품 DB 접근 Mapper
    private final ProductMapper productMapper;

    // 상품 우대조건 저장 처리
    private final ProductPreferentialConditionService productPreferentialConditionService;

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
            // 예금 상품 저장
            productMapper.saveDepositProduct(product);

            // 저장된 예금 상품 ID 조회
            Long productId = productMapper.getDepositProductId(
                    product.getFinancialCompanyNumber(),
                    product.getProductCode()
            );

            // 예금 상푸 우대조건 저장
            productPreferentialConditionService.replacePreferentialConditions(
                    productId,
                    product.getPreferentialConditions()
            );
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

    // 예금 상품 목록 조회
    @Transactional(readOnly = true)
    public ProductListResponseDto getDepositProductList(
            ProductListRequestDto request
    ) {
        // 요청값을 목록 조회 기준에 맞게 정리
        int page = normalizePage(request.getPage());
        int size = normalizeSize(request.getSize());
        int savingTerm = normalizeSavingTerm(request.getSavingTerm());
        String keyword = normalizeKeyword(request.getKeyword());
        int sortCode = convertSortCode(request.getSort());

        // 조회를 시작할 행 위치 계산
        int offset = (page - 1) * size;

        // 현재 페이지의 예금 상품 목록 조회
        List<ProductListItemResponseDto> content =
                productMapper.getProductList(
                        DEPOSIT_PRODUCT_TYPE,
                        keyword,
                        savingTerm,
                        null,
                        sortCode,
                        offset,
                        size
                        );

        // 조건에 맞는 전체 예금 상품 수 조회
        long totalElements =
                productMapper.countProductList(
                        DEPOSIT_PRODUCT_TYPE,
                        keyword,
                        savingTerm,
                        null
                        );

        // 전체 페이지 수 계산
        int totalPages = calculateTotalPages(totalElements, size);

        // 페이지 응답 생성
        ProductListResponseDto response = new ProductListResponseDto();
        response.setContent(content);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);

        return response;
    }

    // 페이지 번호를 정상 범위로 보정
    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    // 페이지당 조회 개수를 정상 범위로 보정
    private int normalizeSize(Integer size) {
        if(size == null || size < 1){
            return 5;
        }

        return Math.min(size, 100);
    }

    // 가입 기간을 정상 범위로 보정
    private int normalizeSavingTerm(Integer savingTerm) {
        return savingTerm == null || savingTerm < 1 ? 12 : savingTerm;
    }

    // 검색어 앞뒤 공백 제거
    private String normalizeKeyword(String keyword){
        if(keyword == null || keyword.trim().isEmpty()){
            return null;
        }

        return keyword.trim();
    }

    // 정렬 문자열을  안전한 정렬  코드로 변환
    private int convertSortCode(String sort) {
        if (sort == null){
            return 1;
        }

        switch (sort) {
            case "maximumInterestRate,asc":
                return 2;
            case "interestRate,desc":
                return 3;
            case "interestRate,asc":
                return 4;
            case "productName,asc":
                return 5;
            case "financialCompanyName,asc":
                return 6;
            case "maximumInterestRate,desc":
            default:
                return 1;

        }
    }

    // 전체 페이지 수 계산
    private int calculateTotalPages(long totalElements, int size){
        if(totalElements == 0){
            return 0;
        }

        return (int) ((totalElements + size- 1) / size);
    }
}
