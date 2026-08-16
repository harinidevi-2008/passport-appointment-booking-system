USE passport_db;

SET @review_note_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'passport_applications'
      AND COLUMN_NAME = 'review_note'
);

SET @review_note_sql = IF(
    @review_note_exists = 0,
    'ALTER TABLE passport_applications ADD COLUMN review_note TEXT NULL AFTER status',
    'SELECT ''review_note already exists'' AS message'
);

PREPARE review_note_stmt FROM @review_note_sql;
EXECUTE review_note_stmt;
DEALLOCATE PREPARE review_note_stmt;
