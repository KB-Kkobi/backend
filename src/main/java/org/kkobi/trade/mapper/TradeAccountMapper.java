package org.kkobi.trade.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kkobi.trade.dto.TradeAccountDto;

@Mapper
public interface TradeAccountMapper {

    TradeAccountDto findByUserId(@Param("userId") Long userId);

    TradeAccountDto findByAccountId(@Param("accountId") Long accountId);

    TradeAccountDto findByAccountIdForUpdate(@Param("accountId") Long accountId);

    int decreaseCashBalance(@Param("accountId") Long accountId, @Param("amount") Long amount);

    int increaseCashBalance(@Param("accountId") Long accountId, @Param("amount") Long amount);

    int increaseLocked(@Param("accountId") Long accountId, @Param("amount") Long amount);

    int decreaseLocked(@Param("accountId") Long accountId, @Param("amount") Long amount);


}
