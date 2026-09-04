USE passport_db;

CREATE TABLE IF NOT EXISTS application_status_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    application_id INT NOT NULL,
    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    changed_by_user_id INT NULL,
    note VARCHAR(255) NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_application_status_history_application
        FOREIGN KEY (application_id)
        REFERENCES passport_applications(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_application_status_history_user
        FOREIGN KEY (changed_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,
    INDEX idx_application_status_history_application (application_id),
    INDEX idx_application_status_history_changed_at (changed_at)
);
