-- =========================================================
-- accounts.last_quiz_date 추가
-- 일일 금융 퀴즈 하루 1회 참여 제한을 위해 마지막 참여일을 기록
-- =========================================================
SET @last_quiz_date_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'accounts'
      AND COLUMN_NAME = 'last_quiz_date'
);

SET @add_last_quiz_date = IF(
    @last_quiz_date_exists = 0,
    'ALTER TABLE accounts ADD COLUMN last_quiz_date DATE NULL AFTER locked_cash',
    'SELECT 1'
);
PREPARE stmt FROM @add_last_quiz_date;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;