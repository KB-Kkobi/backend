ALTER TABLE personas
    ADD COLUMN portfolio_reason_first VARCHAR(500) NULL AFTER reason_summary,
    ADD COLUMN portfolio_reason_second VARCHAR(500) NULL AFTER portfolio_reason_first;
