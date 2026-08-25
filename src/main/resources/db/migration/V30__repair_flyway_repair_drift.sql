-- V25/V26이 flyway_schema_history에는 success=1로 기록됐지만, 배포 파이프라인이
-- migrate 전에 항상 flyway-repair를 실행해 체크섬만 맞춰지고 실제 DDL은 반영되지 않았다.
-- users.profile_image는 이미 운영 DB에 수동으로 반영해뒀으므로 조건부로 재실행해도 안전하게 둔다.
-- MySQL 8.0은 ADD COLUMN IF NOT EXISTS를 지원하지 않으므로 information_schema 패턴 사용 (V8, V10 방식과 동일).
SET @profile_image_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'profile_image'
);

SET @add_profile_image = IF(
    @profile_image_exists = 0,
    'ALTER TABLE users ADD COLUMN profile_image VARCHAR(40) NOT NULL DEFAULT ''SLEEP_KKOBI'' AFTER birth_date',
    'SELECT 1'
);

PREPARE stmt FROM @add_profile_image;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TABLE IF EXISTS logs;
