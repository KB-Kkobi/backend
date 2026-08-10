-- 이전 브랜치에서 적용된 인덱스/컬럼 제거 후 재생성
ALTER TABLE securities DROP INDEX idx_securities_kis_code;
ALTER TABLE securities DROP INDEX uk_securities_ticker;
ALTER TABLE securities DROP COLUMN kis_code;

-- 실시간 시세 조회 대상이 아닌 특수상품(공모펀드 F..., 채권 등) 제거.
-- KRX 상장 종목코드는 6자리 [0-9A-Z] 형식이며, 이 이외는 실시간 시세 파이프라인에서 다루지 않는다.
-- FK 자식(시세 이력)을 먼저 정리한 뒤 부모 행을 삭제한다.
DELETE dp
FROM security_daily_prices dp
JOIN securities s ON s.security_id = dp.security_id
WHERE s.ticker NOT REGEXP '^[0-9A-Z]{6}$';

DELETE FROM securities
WHERE ticker NOT REGEXP '^[0-9A-Z]{6}$';

-- KIS Open API 조회에 사용할 종목코드 컬럼 추가 후 남은 상장 종목 전량 백필.
ALTER TABLE securities
    ADD COLUMN kis_code VARCHAR(20) NULL AFTER ticker;

UPDATE securities
SET kis_code = ticker;

ALTER TABLE securities ADD CONSTRAINT uk_securities_ticker UNIQUE (ticker);

CREATE INDEX idx_securities_kis_code ON securities (kis_code);
