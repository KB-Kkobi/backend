package org.kkobi.product.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.product.holding.dto.ProductHoldingCreateDto;
import org.kkobi.product.holding.dto.ProductSubscriptionInfoDto;

import java.math.BigDecimal;

public interface ProductHoldingMapper {

    // 로그인 사용자의 계좌와 가입할 상품 옵션 정보를 조회
    ProductSubscriptionInfoDto getProductSubscriptionInfo(
            @Param("userId") Long userId,
            @Param("productOptionId") Long productOptionId
    );

    // 가입한 예적금 상품을 보유 상품으로 저장
    int saveHoldingProduct(
            ProductHoldingCreateDto holdingProduct
    );

    // 가입 금액만큼 계좌 현금 잔액 차감
    int decreaseAccountCashBalance(
            @Param("accountId") Long accountId,
            @Param("amount")BigDecimal amount
    );

    // 예적금 가입 거래 내역 저장
    int saveProductSubscriptionTransaction(
            @Param("holdingProductId") Long holdingProductId,
            @Param("amount") BigDecimal amounnt,
            @Param("installmentNumber") Integer installmentNumber
    );

    // 계좌 출금 거래 내역 저장
    int saveAccountWithdrawalTransaction(
            @Param("accountId") Long accountId,
            @Param("amount") BigDecimal amount
    );
}
