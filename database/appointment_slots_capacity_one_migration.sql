USE passport_db;

-- Run once for existing databases so every appointment slot allows one active booking.
ALTER TABLE appointment_slots
    MODIFY capacity INT NOT NULL DEFAULT 1;

UPDATE appointment_slots
SET capacity = 1
WHERE capacity <> 1;
