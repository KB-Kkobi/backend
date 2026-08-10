-- 이전 브랜치에서 적용된 인덱스/컬럼 제거 후 재생성
-- ※ 운영 DB에는 해당 객체가 없으므로 모든 DROP/ADD를 조건부로 처리한다.

-- [1] idx_securities_kis_code 인덱스 조건부 제거
-- V1~V7 어디서도 생성된 적 없음; 로컬 브랜치 환경에만 존재할 수 있으므로 IF EXISTS 패턴 적용
SET @idx_kis_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'securities'
      AND INDEX_NAME   = 'idx_securities_kis_code'
);
SET @drop_idx_kis = IF(
    @idx_kis_exists > 0,
    'ALTER TABLE securities DROP INDEX idx_securities_kis_code',
    'SELECT 1'
);
PREPARE stmt FROM @drop_idx_kis;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- [2] uk_securities_ticker 유니크 인덱스 조건부 제거
-- V1 테이블 정의에 UNIQUE 제약 없음; 동일하게 조건부 처리
SET @uk_ticker_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'securities'
      AND INDEX_NAME   = 'uk_securities_ticker'
);
SET @drop_uk_ticker = IF(
    @uk_ticker_exists > 0,
    'ALTER TABLE securities DROP INDEX uk_securities_ticker',
    'SELECT 1'
);
PREPARE stmt FROM @drop_uk_ticker;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- [3] kis_code 컬럼 조건부 제거
-- V1 테이블 정의에 kis_code 컬럼 없음; 동일하게 조건부 처리
SET @col_kis_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'securities'
      AND COLUMN_NAME  = 'kis_code'
);
SET @drop_col_kis = IF(
    @col_kis_exists > 0,
    'ALTER TABLE securities DROP COLUMN kis_code',
    'SELECT 1'
);
PREPARE stmt FROM @drop_col_kis;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 실시간 시세 조회 대상이 아닌 특수상품(공모펀드 F..., 채권 등) 제거.
-- KRX 상장 종목코드는 6자리 [0-9A-Z] 형식이며, 이 이외는 실시간 시세 파이프라인에서 다루지 않는다.
-- FK 자식(시세 이력)을 먼저 정리한 뒤 부모 행을 삭제한다.
DELETE dp
FROM security_daily_prices dp
JOIN securities s ON s.security_id = dp.security_id
WHERE s.ticker NOT REGEXP '^[0-9A-Z]{6}$';

DELETE FROM securities
WHERE ticker NOT REGEXP '^[0-9A-Z]{6}$';

-- [4] kis_code 컬럼 조건부 추가
-- MySQL 8.0은 ADD COLUMN IF NOT EXISTS를 지원하지 않으므로 information_schema 패턴 사용
SET @col_kis_exists2 = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'securities'
      AND COLUMN_NAME  = 'kis_code'
);
SET @add_col_kis = IF(
    @col_kis_exists2 = 0,
    'ALTER TABLE securities ADD COLUMN kis_code VARCHAR(20) NULL AFTER ticker',
    'SELECT 1'
);
PREPARE stmt FROM @add_col_kis;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- kis_code가 NULL인 행만 백필 (재실행 시 이미 채워진 행 보호)
UPDATE securities
SET kis_code = ticker
WHERE kis_code IS NULL;

-- [5] uk_securities_ticker 유니크 제약 조건부 추가
SET @uk_ticker_exists2 = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'securities'
      AND INDEX_NAME   = 'uk_securities_ticker'
);
SET @add_uk_ticker = IF(
    @uk_ticker_exists2 = 0,
    'ALTER TABLE securities ADD CONSTRAINT uk_securities_ticker UNIQUE (ticker)',
    'SELECT 1'
);
PREPARE stmt FROM @add_uk_ticker;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- [6] idx_securities_kis_code 인덱스 조건부 생성
SET @idx_kis_exists2 = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'securities'
      AND INDEX_NAME   = 'idx_securities_kis_code'
);
SET @create_idx_kis = IF(
    @idx_kis_exists2 = 0,
    'CREATE INDEX idx_securities_kis_code ON securities (kis_code)',
    'SELECT 1'
);
PREPARE stmt FROM @create_idx_kis;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
