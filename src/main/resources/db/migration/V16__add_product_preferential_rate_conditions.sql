-- 상품 옵션별 우대조건과 추가금리를 저장하기 위한 테이블 생성
CREATE TABLE product_preferential_rate_conditions (
                                                      product_preferential_rate_condition_id BIGINT NOT NULL AUTO_INCREMENT,
                                                      product_option_id BIGINT NOT NULL,
                                                      condition_type VARCHAR(30) NOT NULL,
                                                      condition_name VARCHAR(255) NOT NULL,
                                                      additional_rate DECIMAL(5,2) NULL,
                                                      selectable BOOLEAN NOT NULL DEFAULT TRUE,
                                                      display_order INT NOT NULL DEFAULT 0,
                                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                          ON UPDATE CURRENT_TIMESTAMP,

                                                      PRIMARY KEY (product_preferential_rate_condition_id),

    -- 상품 옵션 삭제 시 연결된 우대조건도 함께 삭제
                                                      CONSTRAINT fk_preferential_rate_condition_option
                                                          FOREIGN KEY (product_option_id)
                                                              REFERENCES product_options(product_option_id)
                                                              ON DELETE CASCADE
);