package com.pabs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pabs.util.DBConnection;

public class EmailNotificationDAO {

    private static final Logger LOGGER = Logger.getLogger(EmailNotificationDAO.class.getName());

    private static final String INSERT_NOTIFICATION =
            "INSERT INTO email_notifications(user_id, application_id, appointment_id, notification_type, "
                    + "recipient_email, subject, status, error_message) VALUES(?,?,?,?,?,?,?,?)";

    public void record(Integer userId,
                       Integer applicationId,
                       Integer appointmentId,
                       String notificationType,
                       String recipientEmail,
                       String subject,
                       boolean sent,
                       String errorMessage) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(INSERT_NOTIFICATION)
        ) {
            setNullableInt(ps, 1, userId);
            setNullableInt(ps, 2, applicationId);
            setNullableInt(ps, 3, appointmentId);
            ps.setString(4, notificationType);
            ps.setString(5, recipientEmail);
            ps.setString(6, subject);
            ps.setString(7, sent ? "SENT" : "FAILED");
            ps.setString(8, limit(errorMessage, 1000));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.FINE,
                    "Email notification audit record was not stored. Apply database/email_notifications_audit.sql "
                            + "to enable audit tracking. notificationType=" + notificationType
                            + ", recipientEmail=" + recipientEmail,
                    e);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
