package org.kkobi.trade.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kkobi.trade.dto.TradeStockDto;

@Mapper
public interface TradeStockMapper {

    TradeStockDto findById(@Param("securityId") Long securityId);

    // kisCode → securityId 변환용 (TickMatchingEngine에서 사용)
    TradeStockDto findByKisCode(@Param("kisCode") String kisCode);
}
