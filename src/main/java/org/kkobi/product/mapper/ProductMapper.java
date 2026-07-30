package org.kkobi.product.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.product.dto.DepositProductDto;
import org.kkobi.product.dto.DepositProductOptionDto;

public interface ProductMapper {

    // 예금 상품을 저장하거나 기존 상품 정보를 갱신
    public int saveDepositProduct(DepositProductDto product);

    // 금융회사 번호와 상품 코드로 예금 상품 ID 조회
    public long getDepositProductId(
            @Param("financialCompanyNumber") String finalcialCompanyNumber,
            @Param("productCode") String productCode
    );

    // 예금 상품 옵션을 저장하거나 기존 옵션 정보를 갱신
    public int saveDepositProductOption(
            @Param("productId") Long productId,
            @Param("option") DepositProductOptionDto option
    );
}
