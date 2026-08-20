ALTER TABLE personas
    ADD COLUMN reason_title VARCHAR(100) NULL AFTER caution,
    ADD COLUMN reason_prefix VARCHAR(500) NULL AFTER reason_title,
    ADD COLUMN reason_highlight VARCHAR(200) NULL AFTER reason_prefix,
    ADD COLUMN reason_suffix VARCHAR(100) NULL AFTER reason_highlight,
    ADD COLUMN reason_summary VARCHAR(500) NULL AFTER reason_suffix;
