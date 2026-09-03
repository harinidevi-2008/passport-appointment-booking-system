USE passport_db;

SET @appointment_status_check_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'appointments'
      AND CONSTRAINT_NAME = 'chk_appointments_status'
);

SET @drop_appointment_status_check_sql = IF(
    @appointment_status_check_exists = 1,
    'ALTER TABLE appointments DROP CHECK chk_appointments_status',
    'SELECT ''chk_appointments_status does not exist'' AS message'
);

PREPARE drop_appointment_status_check_stmt FROM @drop_appointment_status_check_sql;
EXECUTE drop_appointment_status_check_stmt;
DEALLOCATE PREPARE drop_appointment_status_check_stmt;

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS attended_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP NULL;

UPDATE appointments
SET status = 'NO_SHOW'
WHERE status = 'EXPIRED';

SET @active_application_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'appointments'
      AND INDEX_NAME = 'uq_appointments_active_application'
);

SET @drop_active_application_index_sql = IF(
    @active_application_index_exists = 1,
    'ALTER TABLE appointments DROP INDEX uq_appointments_active_application',
    'SELECT ''uq_appointments_active_application does not exist'' AS message'
);

PREPARE drop_active_application_index_stmt FROM @drop_active_application_index_sql;
EXECUTE drop_active_application_index_stmt;
DEALLOCATE PREPARE drop_active_application_index_stmt;

ALTER TABLE appointments
    MODIFY COLUMN active_application_id INT GENERATED ALWAYS AS (
        CASE
            WHEN status IN ('BOOKED', 'RESCHEDULED', 'ATTENDED') THEN application_id
            ELSE NULL
        END
    ) STORED;

ALTER TABLE appointments
    ADD CONSTRAINT uq_appointments_active_application
        UNIQUE (active_application_id);

ALTER TABLE appointments
    ADD CONSTRAINT chk_appointments_status
        CHECK (status IN ('BOOKED', 'RESCHEDULED', 'ATTENDED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));

UPDATE appointments a
JOIN appointment_slots s ON s.id = a.slot_id
SET a.status = 'NO_SHOW'
WHERE a.status IN ('BOOKED', 'RESCHEDULED')
  AND TIMESTAMP(s.appointment_date, s.end_time) < NOW();
