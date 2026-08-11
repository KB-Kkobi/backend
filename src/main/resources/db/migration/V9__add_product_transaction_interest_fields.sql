-- 예적금 해지 및 만기 이자 정보를 저장
ALTER TABLE product_transactions
    ADD COLUMN interest_amount BIGINT NULL AFTER amount,
    ADD COLUMN interest_tax_amount BIGINT NULL AFTER interest_amount;