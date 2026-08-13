-- security_daily_prices.trade_date 단독 인덱스 추가
-- 주식 리스트 정렬(거래량순·등락률순) 시 최신 15일 기간 필터가 이 인덱스를 사용하여
-- 전체 스캔(2.5M행) 대신 range 스캔(~4만행)으로 처리된다.
-- MySQL은 CREATE INDEX IF NOT EXISTS를 지원하지 않으므로 information_schema로 존재 여부 확인 후 조건부 실행한다.
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE()
      AND table_name   = 'security_daily_prices'
      AND index_name   = 'idx_sdp_trade_date'
);

SET @sql = IF(
    @idx_exists = 0,
    'CREATE INDEX idx_sdp_trade_date ON security_daily_prices (trade_date)',
    'SELECT ''idx_sdp_trade_date already exists, skipped'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
