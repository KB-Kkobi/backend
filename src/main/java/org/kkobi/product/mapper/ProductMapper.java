package org.kkobi.product.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.product.deposit.dto.DepositProductDto;
import org.kkobi.product.deposit.dto.DepositProductOptionDto;
import org.kkobi.product.dto.response.PreferentialRateConditionResponseDto;
import org.kkobi.product.dto.response.ProductDetailResponseDto;
import org.kkobi.product.dto.response.ProductListItemResponseDto;
import org.kkobi.product.dto.response.ProductOptionResponseDto;
import org.kkobi.product.enums.PreferentialConditionType;
import org.kkobi.product.enums.PreferentialRateConditionRole;
import org.kkobi.product.saving.dto.SavingProductDto;
import org.kkobi.product.saving.dto.SavingProductOptionDto;

import java.util.List;

public interface ProductMapper {

    // 예금 상품을 저장하거나 기존 상품 정보를 갱신
    public int saveDepositProduct(DepositProductDto product);

    // 금융회사 번호와 상품 코드로 예금 상품 ID 조회
    public long getDepositProductId(
            @Param("financialCompanyNumber") String financialCompanyNumber,
            @Param("productCode") String productCode
    );

    // 예금 상품 옵션을 저장하거나 기존 옵션 정보를 갱신
    public int saveDepositProductOption(
            @Param("productId") Long productId,
            @Param("option") DepositProductOptionDto option
    );

    // 적금 상품을 저장하거나 기존 상품 정보를 갱신
    public int saveSavingProduct(SavingProductDto product);

    // 금융회사 번호와 상품 코드로 적금 상품 ID 조회
    public long getSavingProductId(
            @Param("financialCompanyNumber") String financialCompanyNumber,
            @Param("productCode") String productCode
    );

    // 적금 상품 옵션을 저장하거나 기존 옵션 정보를 갱신
    public int saveSavingProductOption(
            @Param("productId") Long productId,
            @Param("option")SavingProductOptionDto option
            );

    // 상품 ID와 상품 유형으로 상품 기본 정보를 조회
    ProductDetailResponseDto getProductDetail(
            @Param("productId") Long productId,
            @Param("productType") String productType
    );

    // 상품 ID로 금리 옵션 목록을 조회
    List<ProductOptionResponseDto> getProductOptions(
            @Param("productId") Long productId
    );

    // 상품 옵션에 등록된 우대조건과 추가금리를 조회
    List<PreferentialRateConditionResponseDto> getPreferentialRateConditions(
            @Param("productOptionId") Long productOptionId
    );

    // 조건에 맞는 상품 목록 조회
    List<ProductListItemResponseDto> getProductList(
            @Param("productType") String productType,
            @Param("keyword") String keyword,
            @Param("savingTerms") List<Integer> savingTerms,
            @Param("reserveTypes") List<String> reserveTypes,
            @Param("preferentialConditions")
            List<PreferentialConditionType> preferentialConditions,
            @Param("sortCode") Integer sortCode,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    // 조건에 맞는 전체 상품 수 조회
    Long countProductList(
            @Param("productType") String productType,
            @Param("keyword") String keyword,
            @Param("savingTerms") List<Integer> savingTerms,
            @Param("reserveTypes") List<String> reserveTypes,
            @Param("preferentialConditions")
            List<PreferentialConditionType> preferentialConditions
    );

    // 상품에 저장된 기존 우대조건을 삭제
    int deleteProductPreferentialConditions(
            @Param("productId") Long productId
    );

    // 상품의 우대조건 유형을 저장
    int saveProductPreferentialCondition(
            @Param("productId") Long productId,
            @Param("conditionType") PreferentialConditionType conditionType
    );

    // 상품 옵션에 저장된 기존 우대금리 조건을 삭제
    int deleteProductPreferentialRateConditions(
            @Param("productId") Long productId
    );

    // 상품 옵션별 우대금리 조건을 저장
    int saveProductPreferentialRateCondition(
            @Param("productOptionId") Long productOptionId,
            @Param("conditionType") PreferentialConditionType conditionType,
            @Param("conditionName") String conditionName,
            @Param("additionalRate") java.math.BigDecimal additionalRate,
            @Param("selectable") boolean selectable,
            @Param("displayOrder") int displayOrder,
            @Param("conditionGroupId") Long conditionGroupId,
            @Param("conditionRole") PreferentialRateConditionRole conditionRole
            );
}
