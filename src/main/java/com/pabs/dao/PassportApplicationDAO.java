package com.pabs.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pabs.model.PassportApplication;
import com.pabs.util.DBConnection;

public class PassportApplicationDAO {

    private static final Logger LOGGER = Logger.getLogger(PassportApplicationDAO.class.getName());

    private static final String INSERT_APPLICATION =
            "INSERT INTO passport_applications(application_number, user_id, application_type, passport_mode, "
                    + "full_name, date_of_birth, gender, place_of_birth, father_name, mother_name, phone, email, "
                    + "address, city, state, pincode, status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_APPLICATION_NUMBER =
            "UPDATE passport_applications SET application_number=? WHERE id=?";

    private static final String SELECT_BY_ID =
            "SELECT id, application_number, user_id, application_type, passport_mode, full_name, date_of_birth, "
                    + "gender, place_of_birth, father_name, mother_name, phone, email, address, city, state, "
                    + "pincode, status, review_note, created_at, updated_at FROM passport_applications WHERE id=?";

    private static final String SELECT_BY_USER_ID =
            "SELECT id, application_number, user_id, application_type, passport_mode, full_name, date_of_birth, "
                    + "gender, place_of_birth, father_name, mother_name, phone, email, address, city, state, "
                    + "pincode, status, review_note, created_at, updated_at FROM passport_applications WHERE user_id=? "
                    + "ORDER BY created_at DESC";

    private static final String SELECT_ALL =
            "SELECT id, application_number, user_id, application_type, passport_mode, full_name, date_of_birth, "
                    + "gender, place_of_birth, father_name, mother_name, phone, email, address, city, state, "
                    + "pincode, status, review_note, created_at, updated_at FROM passport_applications "
                    + "ORDER BY created_at DESC";

    private static final String UPDATE_STATUS =
            "UPDATE passport_applications SET status=?, review_note=? WHERE id=? AND status=?";

    private static final String HAS_COMPLETED_APPOINTMENT =
            "SELECT id FROM appointments WHERE application_id=? AND status='COMPLETED' LIMIT 1";

    private static final String LOCK_WITHDRAWABLE_APPLICATION_FOR_USER =
            "SELECT id, status FROM passport_applications "
                    + "WHERE id=? AND user_id=? AND status IN ('SUBMITTED', 'UNDER_REVIEW', 'VERIFIED') "
                    + "FOR UPDATE";

    private static final String WITHDRAW_APPLICATION =
            "UPDATE passport_applications SET status='CANCELLED', review_note=? "
                    + "WHERE id=? AND user_id=? AND status IN ('SUBMITTED', 'UNDER_REVIEW', 'VERIFIED')";

    private static final String CANCEL_ACTIVE_APPOINTMENTS_FOR_WITHDRAWN_APPLICATION =
            "UPDATE appointments SET status='CANCELLED', cancelled_at=CURRENT_TIMESTAMP "
                    + "WHERE application_id=? AND user_id=? AND status IN ('BOOKED', 'RESCHEDULED', 'ATTENDED')";

    private static final String INSERT_STATUS_HISTORY =
            "INSERT INTO application_status_history(application_id, old_status, new_status, changed_by_user_id, note) "
                    + "VALUES(?,?,?,?,?)";

    private static final String SELECT_STATUS_HISTORY =
            "SELECT id, application_id, old_status, new_status, changed_by_user_id, note, changed_at "
                    + "FROM application_status_history WHERE application_id=? ORDER BY changed_at, id";

    public boolean createApplication(PassportApplication application) {
        Connection connection = null;

        try {
            connection = DBConnection.getConnection();
            if (connection == null) {
                LOGGER.warning("Application submission failed: database connection was unavailable.");
                return false;
            }

            connection.setAutoCommit(false);
            String pendingNumber = "PABS-PENDING-" + System.nanoTime();

            try (PreparedStatement ps = connection.prepareStatement(INSERT_APPLICATION, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, pendingNumber);
                ps.setInt(2, application.getUserId());
                ps.setString(3, application.getApplicationType());
                ps.setString(4, application.getPassportMode());
                ps.setString(5, application.getFullName());
                ps.setDate(6, Date.valueOf(application.getDateOfBirth()));
                ps.setString(7, application.getGender());
                ps.setString(8, application.getPlaceOfBirth());
                ps.setString(9, application.getFatherName());
                ps.setString(10, application.getMotherName());
                ps.setString(11, application.getPhone());
                ps.setString(12, application.getEmail());
                ps.setString(13, application.getAddress());
                ps.setString(14, application.getCity());
                ps.setString(15, application.getState());
                ps.setString(16, application.getPincode());
                ps.setString(17, "SUBMITTED");

                if (ps.executeUpdate() == 0) {
                    connection.rollback();
                    LOGGER.warning("Application submission failed: application insert affected zero rows.");
                    return false;
                }

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        connection.rollback();
                        LOGGER.warning("Application submission failed: generated application ID was not returned.");
                        return false;
                    }

                    int id = generatedKeys.getInt(1);
                    String applicationNumber = generateApplicationNumber(id);

                    try (PreparedStatement updatePs = connection.prepareStatement(UPDATE_APPLICATION_NUMBER)) {
                        updatePs.setString(1, applicationNumber);
                        updatePs.setInt(2, id);

                        if (updatePs.executeUpdate() == 0) {
                            connection.rollback();
                            LOGGER.warning("Application submission failed: application number update affected zero rows.");
                            return false;
                        }
                    }

                    application.setId(id);
                    application.setApplicationNumber(applicationNumber);
                    application.setStatus("SUBMITTED");
                    insertStatusHistory(connection, id, null, "SUBMITTED", application.getUserId(),
                            "Application submitted");
                }
            }

            connection.commit();
            return true;

        } catch (SQLException e) {
            rollbackQuietly(connection);
            LOGGER.log(Level.SEVERE,
                "Application submission database failure. sqlState=" + e.getSQLState()
                    + ", errorCode=" + e.getErrorCode()
                    + ", exceptionType=" + e.getClass().getName()
                    + ", message=" + e.getMessage(),
                e);
        } finally {
            closeQuietly(connection);
        }

        return false;
    }

    public PassportApplication getApplicationById(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            if (connection == null) {
                return null;
            }

            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapApplication(rs);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<PassportApplication> getApplicationsByUserId(int userId) {
        List<PassportApplication> applications = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection()) {
            if (connection == null) {
                return applications;
            }

            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_USER_ID)) {
                ps.setInt(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        applications.add(mapApplication(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return applications;
    }

    public List<PassportApplication> getAllApplications() {
        List<PassportApplication> applications = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                applications.add(mapApplication(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return applications;
    }

    public boolean updateApplicationStatus(int applicationId, String currentStatus, String status, String reviewNote) {
        return updateApplicationStatus(applicationId, currentStatus, status, reviewNote, null);
    }

    public boolean updateApplicationStatus(int applicationId,
                                           String currentStatus,
                                           String status,
                                           String reviewNote,
                                           Integer changedByUserId) {
        Connection connection = null;

        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(UPDATE_STATUS)) {
                ps.setString(1, status);
                ps.setString(2, reviewNote);
                ps.setInt(3, applicationId);
                ps.setString(4, currentStatus);

                if (ps.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
            }

            insertStatusHistory(connection, applicationId, currentStatus, status, changedByUserId, reviewNote);
            connection.commit();
            return true;

        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }

        return false;
    }

    public boolean hasCompletedAppointment(int applicationId) {
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(HAS_COMPLETED_APPOINTMENT)
        ) {
            ps.setInt(1, applicationId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean withdrawApplication(int applicationId, int userId, String reviewNote) {
        Connection connection = null;

        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);
            String oldStatus;

            try (PreparedStatement lockPs = connection.prepareStatement(LOCK_WITHDRAWABLE_APPLICATION_FOR_USER)) {
                lockPs.setInt(1, applicationId);
                lockPs.setInt(2, userId);

                try (ResultSet rs = lockPs.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return false;
                    }
                    oldStatus = rs.getString("status");
                }
            }

            try (PreparedStatement cancelAppointmentsPs =
                         connection.prepareStatement(CANCEL_ACTIVE_APPOINTMENTS_FOR_WITHDRAWN_APPLICATION)) {
                cancelAppointmentsPs.setInt(1, applicationId);
                cancelAppointmentsPs.setInt(2, userId);
                cancelAppointmentsPs.executeUpdate();
            }

            try (PreparedStatement withdrawPs = connection.prepareStatement(WITHDRAW_APPLICATION)) {
                withdrawPs.setString(1, reviewNote);
                withdrawPs.setInt(2, applicationId);
                withdrawPs.setInt(3, userId);

                if (withdrawPs.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
            }

            insertStatusHistory(connection, applicationId, oldStatus, "CANCELLED", userId, reviewNote);

            connection.commit();
            return true;

        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }

        return false;
    }

    public List<ApplicationStatusHistory> getStatusHistoryByApplicationId(int applicationId) {
        List<ApplicationStatusHistory> history = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection()) {
            if (connection == null) {
                return history;
            }

            try (PreparedStatement ps = connection.prepareStatement(SELECT_STATUS_HISTORY)) {
                ps.setInt(1, applicationId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        history.add(mapStatusHistory(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return history;
    }

    private String generateApplicationNumber(int id) {
        return String.format("PABS-%d-%05d", Year.now().getValue(), id);
    }

    private PassportApplication mapApplication(ResultSet rs) throws SQLException {
        PassportApplication application = new PassportApplication();
        application.setId(rs.getInt("id"));
        application.setApplicationNumber(rs.getString("application_number"));
        application.setUserId(rs.getInt("user_id"));
        application.setApplicationType(rs.getString("application_type"));
        application.setPassportMode(rs.getString("passport_mode"));
        application.setFullName(rs.getString("full_name"));
        application.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
        application.setGender(rs.getString("gender"));
        application.setPlaceOfBirth(rs.getString("place_of_birth"));
        application.setFatherName(rs.getString("father_name"));
        application.setMotherName(rs.getString("mother_name"));
        application.setPhone(rs.getString("phone"));
        application.setEmail(rs.getString("email"));
        application.setAddress(rs.getString("address"));
        application.setCity(rs.getString("city"));
        application.setState(rs.getString("state"));
        application.setPincode(rs.getString("pincode"));
        application.setStatus(rs.getString("status"));
        application.setReviewNote(rs.getString("review_note"));
        application.setCreatedAt(rs.getTimestamp("created_at"));
        application.setUpdatedAt(rs.getTimestamp("updated_at"));
        return application;
    }

    private ApplicationStatusHistory mapStatusHistory(ResultSet rs) throws SQLException {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setId(rs.getInt("id"));
        history.setApplicationId(rs.getInt("application_id"));
        history.setOldStatus(rs.getString("old_status"));
        history.setNewStatus(rs.getString("new_status"));
        int changedBy = rs.getInt("changed_by_user_id");
        history.setChangedByUserId(rs.wasNull() ? null : changedBy);
        history.setNote(rs.getString("note"));
        history.setChangedAt(rs.getTimestamp("changed_at"));
        return history;
    }

    private void insertStatusHistory(Connection connection,
                                     int applicationId,
                                     String oldStatus,
                                     String newStatus,
                                     Integer changedByUserId,
                                     String note)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_STATUS_HISTORY)) {
            ps.setInt(1, applicationId);
            ps.setString(2, oldStatus);
            ps.setString(3, newStatus);
            if (changedByUserId == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, changedByUserId);
            }
            ps.setString(5, note);
            ps.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ApplicationStatusHistory {
        private int id;
        private int applicationId;
        private String oldStatus;
        private String newStatus;
        private Integer changedByUserId;
        private String note;
        private java.sql.Timestamp changedAt;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(int applicationId) {
            this.applicationId = applicationId;
        }

        public String getOldStatus() {
            return oldStatus;
        }

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public Integer getChangedByUserId() {
            return changedByUserId;
        }

        public void setChangedByUserId(Integer changedByUserId) {
            this.changedByUserId = changedByUserId;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public java.sql.Timestamp getChangedAt() {
            return changedAt;
        }

        public void setChangedAt(java.sql.Timestamp changedAt) {
            this.changedAt = changedAt;
        }
    }
}
