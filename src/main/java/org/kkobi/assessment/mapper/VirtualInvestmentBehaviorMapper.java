package org.kkobi.assessment.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorDto;

import java.sql.Timestamp;
import java.util.List;

public interface VirtualInvestmentBehaviorMapper {

    boolean existsAccountByUserId(
            @Param("accountId") Long accountId,
            @Param("userId") Long userId
    );

    boolean existsSecurityByIdAndStockCode(
            @Param("securityId") Long securityId,
            @Param("stockCode") String stockCode
    );

    boolean existsProductOption(@Param("productOptionId") Long productOptionId);

    List<VirtualInvestmentBehaviorDto> getPreviousVirtualInvestmentBehaviors(
            @Param("accountId") Long accountId,
            @Param("tradedAt") Timestamp tradedAt
    );
}
