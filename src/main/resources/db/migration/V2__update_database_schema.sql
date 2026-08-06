-- =========================================================
-- V2: DB 스키마 변경사항 반영
--
-- 이전 마이그레이션 실패 과정에서 일부 DDL이 적용됐을 수 있으므로
-- 존재 여부를 확인한 후 변경하도록 작성
-- =========================================================


-- =========================================================
-- 1. accounts
-- 사용자당 하나의 계좌만 생성할 수 있도록 UNIQUE 제약조건 추가
-- =========================================================

SET @accounts_user_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'accounts'
      AND INDEX_NAME = 'uk_accounts_user_id'
);

SET @migration_sql = IF(
    @accounts_user_unique_exists = 0,
    'ALTER TABLE accounts ADD CONSTRAINT uk_accounts_user_id UNIQUE (user_id)',
    'SELECT 1'
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- =========================================================
-- 2. securities
-- security_type CHECK 제약조건 제거
-- security_type -> type 컬럼명 변경
-- type CHECK 제약조건 재등록
-- market 컬럼 추가
-- 투자 성향 지수 컬럼 추가
-- =========================================================


-- ---------------------------------------------------------
-- 2-1. security_type을 사용하는 기존 CHECK 제약조건 제거
-- ---------------------------------------------------------

SET @security_type_check_name = (
    SELECT tc.CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.CHECK_CONSTRAINTS cc
      ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
     AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
    WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'securities'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%security_type%'
    LIMIT 1
);

SET @migration_sql = IF(
    @security_type_check_name IS NULL,
    'SELECT 1',
    CONCAT(
        'ALTER TABLE securities DROP CHECK `',
        @security_type_check_name,
        '`'
    )
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- ---------------------------------------------------------
-- 2-2. security_type -> type 컬럼명 변경
-- ---------------------------------------------------------

SET @security_type_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'securities'
      AND COLUMN_NAME = 'security_type'
);

SET @migration_sql = IF(
    @security_type_column_exists > 0,
    'ALTER TABLE securities RENAME COLUMN security_type TO `type`',
    'SELECT 1'
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- ---------------------------------------------------------
-- 2-3. 변경된 type 컬럼에 CHECK 제약조건 재등록
-- ---------------------------------------------------------

SET @securities_type_check_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'securities'
      AND CONSTRAINT_NAME = 'chk_securities_type'
      AND CONSTRAINT_TYPE = 'CHECK'
);

SET @migration_sql = IF(
    @securities_type_check_exists = 0,
    'ALTER TABLE securities
        ADD CONSTRAINT chk_securities_type
        CHECK (`type` IN (''STOCK'', ''EQUITY_ETF'', ''BOND_ETF''))',
    'SELECT 1'
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- ---------------------------------------------------------
-- 2-4. market 컬럼 추가
-- ---------------------------------------------------------

SET @market_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'securities'
      AND COLUMN_NAME = 'market'
);

SET @migration_sql = IF(
    @market_column_exists = 0,
    'ALTER TABLE securities
        ADD COLUMN market VARCHAR(20) NULL AFTER `type`',
    'SELECT 1'
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- ---------------------------------------------------------
-- 2-5. rt_score 컬럼 추가
-- ---------------------------------------------------------

SET @rt_score_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'securities'
      AND COLUMN_NAME = 'rt_score'
);

SET @migration_sql = IF(
    @rt_score_column_exists = 0,
    'ALTER TABLE securities
        ADD COLUMN rt_score DECIMAL(5,2) NULL AFTER average_volume',
    'SELECT 1'
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- ---------------------------------------------------------
-- 2-6. lh_score 컬럼 추가
-- ---------------------------------------------------------

SET @lh_score_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'securities'
      AND COLUMN_NAME = 'lh_score'
);

SET @migration_sql = IF(
    @lh_score_column_exists = 0,
    'ALTER TABLE securities
        ADD COLUMN lh_score DECIMAL(5,2) NULL AFTER rt_score',
    'SELECT 1'
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- ---------------------------------------------------------
-- 2-7. rp_score 컬럼 추가
-- ---------------------------------------------------------

SET @rp_score_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'securities'
      AND COLUMN_NAME = 'rp_score'
);

SET @migration_sql = IF(
    @rp_score_column_exists = 0,
    'ALTER TABLE securities
        ADD COLUMN rp_score DECIMAL(5,2) NULL AFTER lh_score',
    'SELECT 1'
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- ---------------------------------------------------------
-- 2-8. 기존 데이터 점수 초기화
-- ---------------------------------------------------------

UPDATE securities
SET rt_score = COALESCE(rt_score, 0.00),
    lh_score = COALESCE(lh_score, 0.00),
    rp_score = COALESCE(rp_score, 0.00)
WHERE rt_score IS NULL
   OR lh_score IS NULL
   OR rp_score IS NULL;


-- ---------------------------------------------------------
-- 2-9. 점수 컬럼 NOT NULL 적용
-- ---------------------------------------------------------

ALTER TABLE securities
    MODIFY COLUMN rt_score DECIMAL(5,2) NOT NULL,
    MODIFY COLUMN lh_score DECIMAL(5,2) NOT NULL,
    MODIFY COLUMN rp_score DECIMAL(5,2) NOT NULL;


-- =========================================================
-- 3. events
-- 사용하지 않는 events 테이블 삭제
-- =========================================================

DROP TABLE IF EXISTS events;


-- =========================================================
-- 4. action_logs
-- game_month의 기존 1~12 CHECK 제약조건 제거
-- game_month -> game_tick 컬럼명 변경
-- =========================================================


-- ---------------------------------------------------------
-- 4-1. game_month를 사용하는 CHECK 제약조건 제거
-- ---------------------------------------------------------

SET @game_month_check_name = (
    SELECT tc.CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.CHECK_CONSTRAINTS cc
      ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
     AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
    WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'action_logs'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%game_month%'
    LIMIT 1
);

SET @migration_sql = IF(
    @game_month_check_name IS NULL,
    'SELECT 1',
    CONCAT(
        'ALTER TABLE action_logs DROP CHECK `',
        @game_month_check_name,
        '`'
    )
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- ---------------------------------------------------------
-- 4-2. game_month -> game_tick 컬럼명 변경
-- ---------------------------------------------------------

SET @game_month_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'action_logs'
      AND COLUMN_NAME = 'game_month'
);

SET @migration_sql = IF(
    @game_month_column_exists > 0,
    'ALTER TABLE action_logs RENAME COLUMN game_month TO game_tick',
    'SELECT 1'
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;


-- =========================================================
-- 5. users
-- 주소 관련 컬럼 NULL 허용
-- =========================================================

ALTER TABLE users
    MODIFY COLUMN postal_code VARCHAR(10) NULL,
    MODIFY COLUMN address_line1 VARCHAR(255) NULL,
    MODIFY COLUMN address_line2 VARCHAR(255) NULL;