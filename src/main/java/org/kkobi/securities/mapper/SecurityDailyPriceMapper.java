package org.kkobi.securities.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kkobi.securities.dto.SecurityDailyPriceRow;
import org.kkobi.securities.dto.SecurityKisRow;

import java.util.List;

@Mapper
public interface SecurityDailyPriceMapper {
    List<SecurityKisRow> findAllWithKisCode();
    void upsertBatch(@Param("rows") List<SecurityDailyPriceRow> rows);
}
