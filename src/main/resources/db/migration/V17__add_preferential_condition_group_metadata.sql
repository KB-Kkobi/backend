-- 예적금 우대조건의 UI 그룹화 및 역할 구분을 위한 메타데이터 컬럼 추가
ALTER TABLE product_preferential_rate_conditions
    ADD COLUMN condition_group_id BIGINT NULL
        COMMENT '우대조건 UI 그룹 식별자',
    ADD COLUMN condition_role VARCHAR(30) NOT NULL DEFAULT 'STANDALONE'
        COMMENT '우대조건 역할';