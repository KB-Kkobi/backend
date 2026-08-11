-- =========================================================
-- 매매 기능 스키마 마이그레이션 + securities.mdd CHECK 수정
--
-- 1. holding_securities.quantity > 0 CHECK 제거 (전량 매도 시 quantity=0 허용)
-- 2. accounts.locked_cash 추가
-- 3. holding_securities.locked_quantity 추가
-- 4. security_orders.order_price > 0 CHECK 제거
-- 5. security_orders 컬럼 타입 정리 (order_price NULL, status ENUM 확정)
-- 6. account_transactions.type → ENUM('DEPOSIT','WITHDRAW')
-- 7. uk_holding_account_security UNIQUE 제약 추가
-- 8. 인덱스 3개 추가
-- 9. securities.mdd CHECK 수정 (mdd <= 0 → mdd >= 0)
-- =========================================================


-- =========================================================
-- 1. holding_securities.quantity > 0 CHECK 제거
--    전량 매도 시 quantity=0 레코드를 유지해야 하므로 제거
-- =========================================================
SET @chk_holding_qty = (
    SELECT cc.CONSTRAINT_NAME
    FROM information_schema.CHECK_CONSTRAINTS cc
    JOIN information_schema.TABLE_CONSTRAINTS tc
      ON cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
     AND cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
    WHERE tc.TABLE_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'holding_securities'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%quantity%'
    LIMIT 1
);

SET @drop_holding_chk = IF(
    @chk_holding_qty IS NOT NULL,
    CONCAT('ALTER TABLE holding_securities DROP CHECK `', @chk_holding_qty, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @drop_holding_chk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- =========================================================
-- 2. accounts.locked_cash 추가
-- =========================================================
SET @locked_cash_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'accounts'
      AND COLUMN_NAME = 'locked_cash'
);

SET @add_locked_cash = IF(
    @locked_cash_exists = 0,
    'ALTER TABLE accounts ADD COLUMN locked_cash BIGINT NOT NULL DEFAULT 0 AFTER cash_balance',
    'SELECT 1'
);
PREPARE stmt FROM @add_locked_cash;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- =========================================================
-- 3. holding_securities.locked_quantity 추가
-- =========================================================
SET @locked_qty_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'holding_securities'
      AND COLUMN_NAME = 'locked_quantity'
);

SET @add_locked_qty = IF(
    @locked_qty_exists = 0,
    'ALTER TABLE holding_securities ADD COLUMN locked_quantity INT NOT NULL DEFAULT 0 AFTER quantity',
    'SELECT 1'
);
PREPARE stmt FROM @add_locked_qty;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- =========================================================
-- 4. security_orders.order_price > 0 CHECK 제거
--    시장가 주문은 order_price 가 NULL 이므로 불필요
-- =========================================================
SET @chk_order_price = (
    SELECT cc.CONSTRAINT_NAME
    FROM information_schema.CHECK_CONSTRAINTS cc
    JOIN information_schema.TABLE_CONSTRAINTS tc
      ON cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
     AND cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
    WHERE tc.TABLE_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'security_orders'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%order_price%'
    LIMIT 1
);

SET @drop_order_price_chk = IF(
    @chk_order_price IS NOT NULL,
    CONCAT('ALTER TABLE security_orders DROP CHECK `', @chk_order_price, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @drop_order_price_chk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- =========================================================
-- 5. security_orders 컬럼 타입 정리
--    기존 COMPLETED → FILLED 데이터 마이그레이션 선행
-- =========================================================
UPDATE security_orders SET status = 'FILLED' WHERE status = 'COMPLETED';

-- security_id FK 일시 해제 후 NOT NULL 변경, 복구
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'security_orders'
      AND CONSTRAINT_NAME = 'security_orders_ibfk_2'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @drop_fk = IF(
    @fk_exists > 0,
    'ALTER TABLE security_orders DROP FOREIGN KEY security_orders_ibfk_2',
    'SELECT 1'
);
PREPARE stmt FROM @drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE security_orders
    MODIFY COLUMN security_id    BIGINT NOT NULL,
    MODIFY COLUMN order_price    BIGINT NULL
        COMMENT '지정가 주문의 지정 가격. 시장가는 NULL',
    MODIFY COLUMN executed_price BIGINT NULL
        COMMENT '체결 단가. 미체결은 NULL',
    MODIFY COLUMN order_method   ENUM('MARKET','LIMIT') NOT NULL,
    MODIFY COLUMN order_type     ENUM('BUY','SELL') NOT NULL,
    MODIFY COLUMN status         ENUM('PENDING','FILLED','CANCELLED','EXPIRED','REJECTED') NOT NULL;

SET @fk_missing = (
    SELECT COUNT(*) = 0
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'security_orders'
      AND CONSTRAINT_NAME = 'security_orders_ibfk_2'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @add_fk = IF(
    @fk_missing,
    'ALTER TABLE security_orders ADD CONSTRAINT security_orders_ibfk_2 FOREIGN KEY (security_id) REFERENCES securities(security_id)',
    'SELECT 1'
);
PREPARE stmt FROM @add_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- =========================================================
-- 6. account_transactions.type → ENUM('DEPOSIT','WITHDRAW')
--    account_transactions 는 현금 입출금 전용
--    기존 'IN' → 'DEPOSIT', 'OUT' → 'WITHDRAW'
-- =========================================================
UPDATE account_transactions SET type = 'DEPOSIT'  WHERE type = 'IN';
UPDATE account_transactions SET type = 'WITHDRAW' WHERE type = 'OUT';

SET @chk_trans_type = (
    SELECT cc.CONSTRAINT_NAME
    FROM information_schema.CHECK_CONSTRAINTS cc
    JOIN information_schema.TABLE_CONSTRAINTS tc
      ON cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
     AND cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
    WHERE tc.TABLE_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'account_transactions'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
    LIMIT 1
);

SET @drop_trans_chk = IF(
    @chk_trans_type IS NOT NULL,
    CONCAT('ALTER TABLE account_transactions DROP CHECK `', @chk_trans_type, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @drop_trans_chk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE account_transactions
    MODIFY COLUMN type ENUM('DEPOSIT','WITHDRAW') NOT NULL;


-- =========================================================
-- 7. uk_holding_account_security UNIQUE 제약 추가
--    추가 전 중복 데이터 확인 (결과가 비어 있어야 안전)
-- =========================================================
SELECT account_id, security_id, COUNT(*) AS cnt
FROM holding_securities
GROUP BY account_id, security_id
HAVING COUNT(*) > 1;

SET @uk_holding_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'holding_securities'
      AND INDEX_NAME = 'uk_holding_account_security'
);

SET @add_uk_holding = IF(
    @uk_holding_exists = 0,
    'ALTER TABLE holding_securities ADD CONSTRAINT uk_holding_account_security UNIQUE (account_id, security_id)',
    'SELECT 1'
);
PREPARE stmt FROM @add_uk_holding;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- =========================================================
-- 8. 인덱스 추가
--    idx_orders_account_ordered  : 주문 내역 조회용
--    idx_orders_pending_security : 시세 수신 시 PENDING 주문 스캔용
--    idx_holding_account         : 보유종목 목록 조회용
-- =========================================================
SET @idx_orders_account_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'security_orders'
      AND INDEX_NAME = 'idx_orders_account_ordered'
);
SET @add_idx_orders_account = IF(
    @idx_orders_account_exists = 0,
    'CREATE INDEX idx_orders_account_ordered ON security_orders (account_id, ordered_at DESC)',
    'SELECT 1'
);
PREPARE stmt FROM @add_idx_orders_account;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_pending_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'security_orders'
      AND INDEX_NAME = 'idx_orders_pending_security'
);
SET @add_idx_pending = IF(
    @idx_pending_exists = 0,
    'CREATE INDEX idx_orders_pending_security ON security_orders (status, security_id)',
    'SELECT 1'
);
PREPARE stmt FROM @add_idx_pending;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_holding_account_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'holding_securities'
      AND INDEX_NAME = 'idx_holding_account'
);
SET @add_idx_holding = IF(
    @idx_holding_account_exists = 0,
    'CREATE INDEX idx_holding_account ON holding_securities (account_id)',
    'SELECT 1'
);
PREPARE stmt FROM @add_idx_holding;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- =========================================================
-- 9. securities.mdd CHECK 제약 조건 수정
--    기존: mdd IS NULL OR mdd <= 0  (securities_chk_3)
--    변경: mdd IS NULL OR mdd >= 0
-- =========================================================
SET @chk_mdd = (
    SELECT cc.CONSTRAINT_NAME
    FROM information_schema.CHECK_CONSTRAINTS cc
    JOIN information_schema.TABLE_CONSTRAINTS tc
      ON cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
     AND cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
    WHERE tc.TABLE_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'securities'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%mdd%'
    LIMIT 1
);

SET @drop_mdd_chk = IF(
    @chk_mdd IS NOT NULL,
    CONCAT('ALTER TABLE securities DROP CHECK `', @chk_mdd, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @drop_mdd_chk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE securities
    ADD CONSTRAINT securities_mdd_check
        CHECK (mdd IS NULL OR mdd >= 0);
