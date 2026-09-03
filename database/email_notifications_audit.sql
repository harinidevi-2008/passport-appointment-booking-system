USE passport_db;

CREATE TABLE IF NOT EXISTS email_notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    application_id INT NULL,
    appointment_id INT NULL,
    notification_type VARCHAR(80) NOT NULL,
    recipient_email VARCHAR(150) NOT NULL,
    subject VARCHAR(180) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    error_message TEXT NULL,
    CONSTRAINT chk_email_notifications_status
        CHECK (status IN ('SENT', 'FAILED')),
    INDEX idx_email_notifications_user (user_id),
    INDEX idx_email_notifications_application (application_id),
    INDEX idx_email_notifications_appointment (appointment_id),
    INDEX idx_email_notifications_type (notification_type),
    INDEX idx_email_notifications_status (status),
    INDEX idx_email_notifications_sent_at (sent_at)
);
