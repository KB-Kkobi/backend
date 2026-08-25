ALTER TABLE users
    ADD COLUMN profile_image VARCHAR(40) NOT NULL DEFAULT 'SLEEP_KKOBI'
        AFTER birth_date;
