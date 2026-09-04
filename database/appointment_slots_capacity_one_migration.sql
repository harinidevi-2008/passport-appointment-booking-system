USE passport_db;

-- Run once for demo databases so every generated appointment slot allows three active bookings.
ALTER TABLE appointment_slots
    MODIFY capacity INT NOT NULL DEFAULT 3;

UPDATE appointment_slots
SET capacity = 3
WHERE capacity = 1;
