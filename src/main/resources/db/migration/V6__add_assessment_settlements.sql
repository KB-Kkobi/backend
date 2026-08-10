CREATE TABLE assessment_settlements (
    assessment_settlement_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    assessment_period_type VARCHAR(50) NOT NULL,
    period_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (assessment_settlement_id),

    CONSTRAINT fk_assessment_settlements_account
        FOREIGN KEY (account_id)
            REFERENCES accounts(account_id),

    CONSTRAINT uk_assessment_settlements_account_period
        UNIQUE (account_id, assessment_period_type, period_date),

    CONSTRAINT chk_assessment_settlements_period_type
        CHECK (
            assessment_period_type IN (
                'DAILY_ASSESSMENT_BATCH',
                'SEVEN_DAY_ALLOCATION',
                'WEEKLY_TRADE_FREQUENCY',
                'WEEKLY_CASH_RATIO',
                'LONG_SECURITY_HOLDING'
            )
        ),

    CONSTRAINT chk_assessment_settlements_status
        CHECK (status IN ('PROCESSING', 'COMPLETED')),

    CONSTRAINT chk_assessment_settlements_completed_at
        CHECK (
            (status = 'PROCESSING' AND completed_at IS NULL)
            OR (status = 'COMPLETED' AND completed_at IS NOT NULL)
        )
);

CREATE INDEX idx_assessment_settlements_status_updated
    ON assessment_settlements (status, updated_at);
