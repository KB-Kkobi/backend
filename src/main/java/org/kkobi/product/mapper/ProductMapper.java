package org.kkobi.product.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.product.deposit.dto.DepositProductDto;
import org.kkobi.product.deposit.dto.DepositProductOptionDto;
import org.kkobi.product.dto.response.ProductDetailResponseDto;
import org.kkobi.product.dto.response.ProductOptionResponseDto;
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
}
