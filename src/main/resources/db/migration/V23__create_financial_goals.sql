-- =========================================================
-- V23: 회원별 단일 재무 목표
-- =========================================================

CREATE TABLE IF NOT EXISTS financial_goals (
    financial_goal_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    goal_type VARCHAR(30) NOT NULL,
    custom_goal_name VARCHAR(50) NULL,
    target_amount BIGINT NOT NULL,
    target_months INT NOT NULL,
    funding_method VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (financial_goal_id),
    CONSTRAINT uk_financial_goals_user_id UNIQUE (user_id),
    CONSTRAINT fk_financial_goals_user_id
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_financial_goals_goal_type
        CHECK (goal_type IN (
            'LUMP_SUM', 'TRAVEL', 'ELECTRONICS', 'EDUCATION',
            'HOUSING', 'EMERGENCY', 'OTHER'
        )),
    CONSTRAINT chk_financial_goals_target_amount
        CHECK (target_amount BETWEEN 1 AND 1000000000000),
    CONSTRAINT chk_financial_goals_target_months
        CHECK (target_months BETWEEN 1 AND 600),
    CONSTRAINT chk_financial_goals_funding_method
        CHECK (funding_method IN ('LUMP_SUM', 'MONTHLY')),
    CONSTRAINT chk_financial_goals_custom_name
        CHECK (
            (goal_type = 'OTHER' AND custom_goal_name IS NOT NULL AND CHAR_LENGTH(TRIM(custom_goal_name)) > 0)
            OR (goal_type <> 'OTHER' AND custom_goal_name IS NULL)
        )
);
