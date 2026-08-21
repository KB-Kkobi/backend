-- =========================================================
-- V24: 목표 준비 방식을 현재 마련 금액으로 교체
-- =========================================================

ALTER TABLE financial_goals
    ADD COLUMN current_amount BIGINT NOT NULL DEFAULT 0 AFTER target_amount,
    ADD CONSTRAINT chk_financial_goals_current_amount
        CHECK (current_amount BETWEEN 0 AND 1000000000000),
    DROP CHECK chk_financial_goals_funding_method,
    DROP COLUMN funding_method;
