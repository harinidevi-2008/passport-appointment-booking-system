USE passport_db;

CREATE TABLE IF NOT EXISTS passport_offices (
    id INT PRIMARY KEY AUTO_INCREMENT,
    office_name VARCHAR(150) NOT NULL,
    office_type VARCHAR(10) NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    address VARCHAR(255) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_passport_offices_office_type
        CHECK (office_type IN ('PSK', 'POPSK')),
    INDEX idx_passport_offices_city_state (city, state),
    INDEX idx_passport_offices_active (active)
);

CREATE TABLE IF NOT EXISTS appointment_slots (
    id INT PRIMARY KEY AUTO_INCREMENT,
    office_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_slots_office
        FOREIGN KEY (office_id)
        REFERENCES passport_offices(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_appointment_slots_capacity
        CHECK (capacity > 0),
    CONSTRAINT chk_appointment_slots_time_range
        CHECK (start_time < end_time),
    CONSTRAINT uq_appointment_slots_office_date_start
        UNIQUE (office_id, appointment_date, start_time),
    INDEX idx_appointment_slots_date (appointment_date),
    INDEX idx_appointment_slots_office (office_id),
    INDEX idx_appointment_slots_office_date_active (office_id, appointment_date, active)
);

CREATE TABLE IF NOT EXISTS appointments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    appointment_number VARCHAR(30) NOT NULL,
    application_id INT NOT NULL,
    user_id INT NOT NULL,
    slot_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_application_id INT GENERATED ALWAYS AS (
        CASE
            WHEN status IN ('BOOKED', 'RESCHEDULED') THEN application_id
            ELSE NULL
        END
    ) STORED,
    CONSTRAINT fk_appointments_application
        FOREIGN KEY (application_id)
        REFERENCES passport_applications(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_slot
        FOREIGN KEY (slot_id)
        REFERENCES appointment_slots(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_appointments_status
        CHECK (status IN ('BOOKED', 'RESCHEDULED', 'CANCELLED', 'COMPLETED', 'EXPIRED')),
    CONSTRAINT uq_appointments_appointment_number
        UNIQUE (appointment_number),
    CONSTRAINT uq_appointments_active_application
        UNIQUE (active_application_id),
    INDEX idx_appointments_application (application_id),
    INDEX idx_appointments_user (user_id),
    INDEX idx_appointments_slot (slot_id),
    INDEX idx_appointments_status (status),
    INDEX idx_appointments_booked_at (booked_at),
    INDEX idx_appointments_user_status (user_id, status),
    INDEX idx_appointments_application_status (application_id, status)
);
