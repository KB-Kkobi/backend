package org.kkobi.product.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.product.holding.dto.ProductHoldingCreateDto;
import org.kkobi.product.holding.dto.ProductSubscriptionInfoDto;
import org.kkobi.product.holding.dto.ProductHoldingInfoDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    // 로그인 사용자의 보유 예적금 목록 조회
    List<ProductHoldingInfoDto> getHoldingProductsByUserId(
            @Param("userId") Long userId
    );

    // 로그인 사용자의 해지 대상 예적금 조회
    ProductHoldingInfoDto getHoldingProductForTermination(
            @Param("userId") Long userId,
            @Param("holdingProductId") Long holdingProductId
    );

    // 해지 반환 금액만큼 계좌 현금 잔액 증가
    int increaseAccountCashBalance(
            @Param("accountId") Long accountId,
            @Param("amount") BigDecimal amount
    );

    // 보유 예적금 상태를 해지로 변경
    int terminateHoldingProduct(
            @Param("holdingProductId") Long holdingProductId
    );

    // 예적금 해지 거래 내역 저장
    int saveProductTerminationTransaction(
            @Param("holdingProductId") Long holdingProductId,
            @Param("amount") BigDecimal amount,
            @Param("interestAmount") BigDecimal interestAmount,
            @Param("interestTaxAmount") BigDecimal interestTaxAmount,
            @Param("terminatedAt")LocalDateTime terminatedAt
            );

    // 예적금 해지로 발생한 계좌 입금 거래 내역 저장
    int saveAccountDepositTransaction(
            @Param("accountId") Long accountId,
            @Param("amount") BigDecimal amount
    );

    // 해당 연도 누적 이자소득 조회
    BigDecimal getAnnualInterestIncome(
            @Param("userId") Long userId,
            @Param("yearsStart") LocalDateTime yearStart,
            @Param("nextYearStart") LocalDateTime nextYearStart
    );

}
