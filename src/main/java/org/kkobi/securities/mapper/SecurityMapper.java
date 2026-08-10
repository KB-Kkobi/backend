package org.kkobi.securities.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kkobi.securities.dto.response.SecurityDetailResponse;
import org.kkobi.securities.dto.response.SecurityListItemResponse;
import org.kkobi.securities.dto.response.TickerKisCodeRow;
import org.kkobi.securities.enums.SecurityType;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SecurityMapper {

    // 종목 리스트 조회 (type 필터, keyword 검색, 페이지네이션)
    List<SecurityListItemResponse> getSecurityList(
            @Param("type") SecurityType type,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    // 종목 리스트 조건에 맞는 전체 개수 조회
    long countSecurityList(
            @Param("type") SecurityType type,
            @Param("keyword") String keyword);

    // ticker로 종목 상세 조회 (없으면 null)
    SecurityDetailResponse getSecurityByTicker(@Param("ticker") String ticker);

    // 여러 ticker에 대해 (ticker, kis_code) 배치 조회
    List<TickerKisCodeRow> findKisCodesByTickers(@Param("tickers") Collection<String> tickers);
}
