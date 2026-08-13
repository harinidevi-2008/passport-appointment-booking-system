package com.pabs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import com.pabs.model.Appointment;
import com.pabs.util.DBConnection;

public class AppointmentDAO {

    private static final String ACTIVE_APPOINTMENT_STATUSES = "'BOOKED', 'RESCHEDULED'";

    private static final String SELECT_COLUMNS =
            "id, appointment_number, application_id, user_id, slot_id, status, booked_at, "
                    + "cancelled_at, created_at, updated_at";

    private static final String VERIFY_APPLICATION_OWNERSHIP =
            "SELECT id FROM passport_applications WHERE id=? AND user_id=?";

    private static final String FIND_ACTIVE_FOR_APPLICATION =
            "SELECT " + SELECT_COLUMNS + " FROM appointments "
                    + "WHERE application_id=? AND user_id=? "
                    + "AND status IN (" + ACTIVE_APPOINTMENT_STATUSES + ") "
                    + "ORDER BY booked_at DESC LIMIT 1";

    private static final String LOCK_ACTIVE_SLOT =
            "SELECT id, capacity, appointment_date, start_time "
                    + "FROM appointment_slots WHERE id=? AND active=TRUE FOR UPDATE";

    private static final String SELECT_SLOT_SCHEDULE =
            "SELECT appointment_date, start_time FROM appointment_slots WHERE id=?";

    private static final String COUNT_ACTIVE_BOOKINGS_FOR_SLOT =
            "SELECT COUNT(*) FROM appointments WHERE slot_id=? "
                    + "AND status IN (" + ACTIVE_APPOINTMENT_STATUSES + ")";

    private static final String INSERT_APPOINTMENT =
            "INSERT INTO appointments(appointment_number, application_id, user_id, slot_id, status) "
                    + "VALUES(?,?,?,?,?)";

    private static final String UPDATE_APPOINTMENT_NUMBER =
            "UPDATE appointments SET appointment_number=? WHERE id=?";

    private static final String SELECT_BY_ID =
            "SELECT " + SELECT_COLUMNS + " FROM appointments WHERE id=?";

    private static final String SELECT_BY_USER_ID =
            "SELECT " + SELECT_COLUMNS + " FROM appointments WHERE user_id=? "
                    + "ORDER BY created_at DESC";

    private static final String SELECT_BY_ID_FOR_USER =
            "SELECT " + SELECT_COLUMNS + " FROM appointments WHERE id=? AND user_id=?";

    private static final String LOCK_ACTIVE_APPOINTMENT_FOR_USER =
            "SELECT " + SELECT_COLUMNS + " FROM appointments "
                    + "WHERE id=? AND user_id=? AND status IN (" + ACTIVE_APPOINTMENT_STATUSES + ") "
                    + "FOR UPDATE";

    private static final String CANCEL_APPOINTMENT =
            "UPDATE appointments SET status='CANCELLED', cancelled_at=CURRENT_TIMESTAMP "
                    + "WHERE id=? AND user_id=? AND status IN (" + ACTIVE_APPOINTMENT_STATUSES + ")";

    private static final String UPDATE_APPOINTMENT_SLOT =
            "UPDATE appointments SET slot_id=?, status='RESCHEDULED' WHERE id=?";

    public Appointment createAppointment(int applicationId, int userId, int slotId) {
        Connection connection = null;

        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);

            if (!applicationBelongsToUser(connection, applicationId, userId)) {
                connection.rollback();
                return null;
            }

            if (findActiveAppointmentForApplication(connection, applicationId, userId) != null) {
                connection.rollback();
                return null;
            }

            int capacity = lockActiveSlotAndGetCapacity(connection, slotId);
            if (capacity <= 0) {
                connection.rollback();
                return null;
            }

            int activeBookings = countActiveBookingsForSlot(connection, slotId);
            if (activeBookings >= capacity) {
                connection.rollback();
                return null;
            }

            int appointmentId = insertAppointment(connection, applicationId, userId, slotId);
            if (appointmentId == 0) {
                connection.rollback();
                return null;
            }

            String appointmentNumber = generateAppointmentNumber(appointmentId);
            updateAppointmentNumber(connection, appointmentId, appointmentNumber);

            Appointment appointment = findById(connection, appointmentId);
            connection.commit();
            return appointment;

        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }

        return null;
    }

    public List<Appointment> findAppointmentsByUserId(int userId) {
        List<Appointment> appointments = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_BY_USER_ID)
        ) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapAppointment(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return appointments;
    }

    public Appointment findByIdForUser(int appointmentId, int userId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID_FOR_USER)
        ) {

            ps.setInt(1, appointmentId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAppointment(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Appointment findActiveAppointmentForApplication(int applicationId, int userId) {

        try (Connection connection = DBConnection.getConnection()) {
            return findActiveAppointmentForApplication(connection, applicationId, userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean cancelAppointment(int appointmentId, int userId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(CANCEL_APPOINTMENT)
        ) {

            ps.setInt(1, appointmentId);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public Appointment rescheduleAppointment(int appointmentId, int userId, int newSlotId) {
        Connection connection = null;

        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);

            Appointment appointment = lockActiveAppointmentForUser(connection, appointmentId, userId);
            if (appointment == null || appointment.getSlotId() == newSlotId) {
                connection.rollback();
                return null;
            }

            if (!isFutureSlot(connection, appointment.getSlotId())) {
                connection.rollback();
                return null;
            }

            int capacity = lockActiveSlotAndGetCapacity(connection, newSlotId);
            if (capacity <= 0) {
                connection.rollback();
                return null;
            }

            int activeBookings = countActiveBookingsForSlot(connection, newSlotId);
            if (activeBookings >= capacity) {
                connection.rollback();
                return null;
            }

            if (!updateAppointmentSlot(connection, appointmentId, newSlotId)) {
                connection.rollback();
                return null;
            }

            Appointment rescheduledAppointment = findById(connection, appointmentId);
            connection.commit();
            return rescheduledAppointment;

        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }

        return null;
    }

    public int countActiveBookingsForSlot(int slotId) {

        try (Connection connection = DBConnection.getConnection()) {
            return countActiveBookingsForSlot(connection, slotId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private boolean applicationBelongsToUser(Connection connection, int applicationId, int userId)
            throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(VERIFY_APPLICATION_OWNERSHIP)) {
            ps.setInt(1, applicationId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Appointment findActiveAppointmentForApplication(Connection connection, int applicationId, int userId)
            throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(FIND_ACTIVE_FOR_APPLICATION)) {
            ps.setInt(1, applicationId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAppointment(rs);
                }
            }
        }

        return null;
    }

    private int lockActiveSlotAndGetCapacity(Connection connection, int slotId) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(LOCK_ACTIVE_SLOT)) {
            ps.setInt(1, slotId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (!isFutureSlot(rs)) {
                        return 0;
                    }
                    return rs.getInt("capacity");
                }
            }
        }

        return 0;
    }

    private Appointment lockActiveAppointmentForUser(Connection connection, int appointmentId, int userId)
            throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(LOCK_ACTIVE_APPOINTMENT_FOR_USER)) {
            ps.setInt(1, appointmentId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAppointment(rs);
                }
            }
        }

        return null;
    }

    private boolean isFutureSlot(Connection connection, int slotId) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(SELECT_SLOT_SCHEDULE)) {
            ps.setInt(1, slotId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && isFutureSlot(rs);
            }
        }
    }

    private boolean isFutureSlot(ResultSet rs) throws SQLException {
        return LocalDateTime.of(
                rs.getDate("appointment_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime()
        ).isAfter(LocalDateTime.now());
    }

    private int countActiveBookingsForSlot(Connection connection, int slotId) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(COUNT_ACTIVE_BOOKINGS_FOR_SLOT)) {
            ps.setInt(1, slotId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    private int insertAppointment(Connection connection, int applicationId, int userId, int slotId)
            throws SQLException {

        String pendingNumber = "TMP-" + System.nanoTime();

        try (PreparedStatement ps = connection.prepareStatement(INSERT_APPOINTMENT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, pendingNumber);
            ps.setInt(2, applicationId);
            ps.setInt(3, userId);
            ps.setInt(4, slotId);
            ps.setString(5, "BOOKED");

            if (ps.executeUpdate() == 0) {
                return 0;
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        return 0;
    }

    private void updateAppointmentNumber(Connection connection, int appointmentId, String appointmentNumber)
            throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(UPDATE_APPOINTMENT_NUMBER)) {
            ps.setString(1, appointmentNumber);
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        }
    }

    private boolean updateAppointmentSlot(Connection connection, int appointmentId, int newSlotId)
            throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(UPDATE_APPOINTMENT_SLOT)) {
            ps.setInt(1, newSlotId);
            ps.setInt(2, appointmentId);
            return ps.executeUpdate() > 0;
        }
    }

    private Appointment findById(Connection connection, int appointmentId) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, appointmentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAppointment(rs);
                }
            }
        }

        return null;
    }

    private String generateAppointmentNumber(int appointmentId) {
        return String.format("APT-%d-%05d", Year.now().getValue(), appointmentId);
    }

    private Appointment mapAppointment(ResultSet rs) throws SQLException {
        Appointment appointment = new Appointment();
        appointment.setId(rs.getInt("id"));
        appointment.setAppointmentNumber(rs.getString("appointment_number"));
        appointment.setApplicationId(rs.getInt("application_id"));
        appointment.setUserId(rs.getInt("user_id"));
        appointment.setSlotId(rs.getInt("slot_id"));
        appointment.setStatus(rs.getString("status"));
        appointment.setBookedAt(rs.getTimestamp("booked_at"));
        appointment.setCancelledAt(rs.getTimestamp("cancelled_at"));
        appointment.setCreatedAt(rs.getTimestamp("created_at"));
        appointment.setUpdatedAt(rs.getTimestamp("updated_at"));
        return appointment;
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
}
