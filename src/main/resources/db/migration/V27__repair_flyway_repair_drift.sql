-- V25/V26이 flyway_schema_history에는 success=1로 기록됐지만, 배포 파이프라인이
-- migrate 전에 항상 flyway-repair를 실행해 체크섬만 맞춰지고 실제 DDL은 반영되지 않았다.
-- users.profile_image는 이미 운영 DB에 수동으로 반영해뒀으므로 IF NOT EXISTS로 재실행해도 안전하게 둔다.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_image VARCHAR(40) NOT NULL DEFAULT 'SLEEP_KKOBI'
        AFTER birth_date;

DROP TABLE IF EXISTS logs;
