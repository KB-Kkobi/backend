-- =========================================================
-- 1. action_logs 시장 상태 제약조건 변경
-- SIDEWAYS, BEAR -> NORMAL
-- =========================================================

SET @market_state_check_name = (
    SELECT tc.CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.CHECK_CONSTRAINTS cc
      ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
     AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
    WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'action_logs'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%market_state%'
    LIMIT 1
);

SET @migration_sql = IF(
    @market_state_check_name IS NULL,
    'SELECT 1',
    CONCAT(
        'ALTER TABLE action_logs DROP CHECK `',
        @market_state_check_name,
        '`'
    )
);

PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

UPDATE action_logs
SET market_state = 'NORMAL'
WHERE market_state IN ('SIDEWAYS', 'BEAR');

ALTER TABLE action_logs
    ADD CONSTRAINT chk_action_logs_market_state
        CHECK (
            market_state IN (
                'BULL',
                'NORMAL',
                'CRASH',
                'VOLATILE'
            )
        );


-- =========================================================
-- 2. action_logs game_tick 제약조건 및 조회 인덱스
-- =========================================================

ALTER TABLE action_logs
    ADD CONSTRAINT chk_action_logs_game_tick
        CHECK (game_tick BETWEEN 0 AND 52);

CREATE INDEX idx_action_logs_user_tick_id
    ON action_logs (user_id, game_tick, action_log_id);


-- =========================================================
-- 3. 가상투자 일별 자산 스냅샷
-- 계좌별로 하루에 하나의 마감 스냅샷 저장
-- =========================================================

CREATE TABLE account_daily_snapshots (
    account_daily_snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,

    current_cash BIGINT NOT NULL,
    current_stock_principal BIGINT NOT NULL,
    current_deposit BIGINT NOT NULL,
    total_invested_principal BIGINT NOT NULL,
    cash_ratio DECIMAL(5,2) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (account_daily_snapshot_id),

    CONSTRAINT fk_account_daily_snapshots_account
        FOREIGN KEY (account_id)
            REFERENCES accounts(account_id),

    CONSTRAINT uk_account_daily_snapshots_account_date
        UNIQUE (account_id, snapshot_date),

    CONSTRAINT chk_account_daily_snapshots_current_cash
        CHECK (current_cash >= 0),

    CONSTRAINT chk_account_daily_snapshots_stock_principal
        CHECK (current_stock_principal >= 0),

    CONSTRAINT chk_account_daily_snapshots_deposit
        CHECK (current_deposit >= 0),

    CONSTRAINT chk_account_daily_snapshots_total_principal
        CHECK (
            total_invested_principal =
                current_cash
                + current_stock_principal
                + current_deposit
        ),

    CONSTRAINT chk_account_daily_snapshots_cash_ratio
        CHECK (
            cash_ratio IS NULL
                OR cash_ratio BETWEEN 0 AND 100
        )
);

CREATE INDEX idx_account_daily_snapshots_date
    ON account_daily_snapshots (snapshot_date);
