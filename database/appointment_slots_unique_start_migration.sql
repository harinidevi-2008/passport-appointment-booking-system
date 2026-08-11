USE passport_db;

-- Run this once for existing databases so concurrent slot generation has a database-level guard.
-- If duplicate office/date/start_time rows already exist, remove or merge them before running this.
SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment_slots'
      AND index_name = 'uq_appointment_slots_office_date_start'
);

SET @sql = IF(
    @index_exists = 0,
    'ALTER TABLE appointment_slots ADD UNIQUE INDEX uq_appointment_slots_office_date_start (office_id, appointment_date, start_time)',
    'SELECT ''uq_appointment_slots_office_date_start already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
