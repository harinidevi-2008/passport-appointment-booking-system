USE passport_db;

-- Run once after deploying the capacity-3 appointment slot change.
-- This keeps the schema default aligned with the application and updates old demo slots
-- that were generated with the previous one-person default.
ALTER TABLE appointment_slots
    MODIFY capacity INT NOT NULL DEFAULT 3;

UPDATE appointment_slots
SET capacity = 3
WHERE capacity = 1;
