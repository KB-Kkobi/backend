package org.kkobi.trade.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kkobi.trade.dto.HoldingDto;

import java.util.List;

@Mapper
public interface HoldingMapper {

    HoldingDto findByAccountAndSecurity(
            @Param("accountId") Long accountId,
            @Param("securityId") Long securityId);

    List<HoldingDto> findAllByAccountId(@Param("accountId") Long accountId);

    int insert(HoldingDto holding);

    int updateQuantityAndAvgPrice(
            @Param("holdingSecurityId") Long holdingSecurityId,
            @Param("quantity") Integer quantity,
            @Param("averagePrice") Long averagePrice);

    int decreaseQuantity(
            @Param("holdingSecurityId") Long holdingSecurityId,
            @Param("quantity") Integer quantity);

    int increaseLocked(
            @Param("holdingSecurityId") Long holdingSecurityId,
            @Param("amount") Integer amount);

    int decreaseLocked(
            @Param("holdingSecurityId") Long holdingSecurityId,
            @Param("amount") Integer amount);

    Long sumStockPrincipalByAccountId(@Param("accountId") Long accountId);
}
