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
    ADD CONSTRAINT chk_appointments_status
        CHECK (status IN ('BOOKED', 'RESCHEDULED', 'CANCELLED', 'COMPLETED', 'EXPIRED'));

UPDATE appointments a
JOIN appointment_slots s ON s.id = a.slot_id
SET a.status = 'EXPIRED'
WHERE a.status IN ('BOOKED', 'RESCHEDULED')
  AND TIMESTAMP(s.appointment_date, s.end_time) < NOW();
