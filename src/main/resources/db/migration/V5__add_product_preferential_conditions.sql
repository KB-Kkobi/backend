-- =========================================================
-- 1. 예적금 상품 우대조건
-- 상품별 우대조건을 필터링 가능한 형태로 저장
-- =========================================================

CREATE TABLE product_preferential_conditions (
                                                 product_preferential_condition_id BIGINT NOT NULL AUTO_INCREMENT,
                                                 product_id BIGINT NOT NULL,
                                                 condition_type VARCHAR(30) NOT NULL,
                                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                 PRIMARY KEY (product_preferential_condition_id),

                                                 CONSTRAINT fk_product_preferential_conditions_product
                                                     FOREIGN KEY (product_id)
                                                         REFERENCES products(product_id)
                                                         ON DELETE CASCADE,

                                                 CONSTRAINT uk_product_preferential_conditions_product_type
                                                     UNIQUE (product_id, condition_type),

                                                 CONSTRAINT chk_product_preferential_conditions_type
                                                     CHECK (
                                                         condition_type IN (
                                                                            'INCOME_TRANSFER',
                                                                            'CARD_USAGE',
                                                                            'AUTOMATIC_TRANSFER',
                                                                            'FIRST_TRANSACTION',
                                                                            'MARKETING_CONSENT',
                                                                            'HOUSING_SUBSCRIPTION',
                                                                            'OPEN_BANKING',
                                                                            'NON_FACE_TO_FACE',
                                                                            'OTHER'
                                                             )
                                                         )
);

CREATE INDEX idx_product_preferential_conditions_type
    ON product_preferential_conditions (condition_type);