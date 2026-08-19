ALTER TABLE personas
    ADD COLUMN portfolio_stock_reason VARCHAR(500) NULL AFTER portfolio_reason_second,
    ADD COLUMN portfolio_bond_reason VARCHAR(500) NULL AFTER portfolio_stock_reason,
    ADD COLUMN portfolio_deposit_reason VARCHAR(500) NULL AFTER portfolio_bond_reason;

