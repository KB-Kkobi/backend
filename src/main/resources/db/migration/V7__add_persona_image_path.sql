-- 페르소나 이미지 경로 컬럼을 추가
ALTER TABLE personas
    ADD COLUMN image_path VARCHAR(255) NULL
COMMENT '페르소나 이미지 경로'
AFTER persona_name;