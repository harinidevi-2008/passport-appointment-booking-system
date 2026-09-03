package com.pabs.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.pabs.model.AppointmentSlot;
import com.pabs.util.DBConnection;

public class AppointmentSlotDAO {

    private static final String ACTIVE_APPOINTMENT_STATUSES = "'BOOKED', 'RESCHEDULED'";
    private static final String EXPIRE_PAST_ACTIVE_APPOINTMENTS =
            "UPDATE appointments a JOIN appointment_slots s ON s.id=a.slot_id "
                    + "SET a.status='NO_SHOW' "
                    + "WHERE a.status IN (" + ACTIVE_APPOINTMENT_STATUSES + ") "
                    + "AND TIMESTAMP(s.appointment_date, s.end_time) < NOW()";
    private static final int DEFAULT_SLOT_CAPACITY = 1;

    private static final LocalTime[][] STANDARD_SLOT_TIMES = {
            {LocalTime.of(9, 0), LocalTime.of(9, 30)},
            {LocalTime.of(9, 30), LocalTime.of(10, 0)},
            {LocalTime.of(10, 0), LocalTime.of(10, 30)},
            {LocalTime.of(10, 30), LocalTime.of(11, 0)},
            {LocalTime.of(11, 0), LocalTime.of(11, 30)}
    };

    private static final String SELECT_COLUMNS =
            "id, office_id, appointment_date, start_time, end_time, capacity, active, created_at";

    private static final String SELECT_AVAILABLE_ACTIVE_SLOTS =
            "SELECT s." + SELECT_COLUMNS.replace(", ", ", s.") + " "
                    + "FROM appointment_slots s "
                    + "LEFT JOIN appointments a ON a.slot_id=s.id "
                    + "AND a.status IN (" + ACTIVE_APPOINTMENT_STATUSES + ") "
                    + "AND TIMESTAMP(s.appointment_date, s.end_time) >= NOW() "
                    + "WHERE s.office_id=? AND s.appointment_date=? AND s.active=TRUE "
                    + "GROUP BY s.id, s.office_id, s.appointment_date, s.start_time, s.end_time, "
                    + "s.capacity, s.active, s.created_at "
                    + "HAVING COUNT(a.id) < s.capacity "
                    + "ORDER BY s.start_time";

    private static final String SELECT_BY_ID =
            "SELECT " + SELECT_COLUMNS + " FROM appointment_slots WHERE id=?";

    private static final String COUNT_ACTIVE_BOOKINGS =
            "SELECT COUNT(*) FROM appointments a JOIN appointment_slots s ON s.id=a.slot_id "
                    + "WHERE a.slot_id=? "
                    + "AND a.status IN (" + ACTIVE_APPOINTMENT_STATUSES + ") "
                    + "AND TIMESTAMP(s.appointment_date, s.end_time) >= NOW()";

    private static final String HAS_AVAILABLE_CAPACITY =
            "SELECT s.capacity, COUNT(a.id) AS active_bookings "
                    + "FROM appointment_slots s "
                    + "LEFT JOIN appointments a ON a.slot_id=s.id "
                    + "AND a.status IN (" + ACTIVE_APPOINTMENT_STATUSES + ") "
                    + "AND TIMESTAMP(s.appointment_date, s.end_time) >= NOW() "
                    + "WHERE s.id=? AND s.active=TRUE "
                    + "GROUP BY s.id, s.capacity";

    private static final String LOCK_ACTIVE_OFFICE =
            "SELECT id FROM passport_offices WHERE id=? AND active=TRUE FOR UPDATE";

    private static final String SELECT_STANDARD_SLOT_START_TIMES =
            "SELECT start_time FROM appointment_slots "
                    + "WHERE office_id=? AND appointment_date=? "
                    + "AND start_time IN (?, ?, ?, ?, ?)";

    private static final String INSERT_SLOT =
            "INSERT IGNORE INTO appointment_slots"
                    + "(office_id, appointment_date, start_time, end_time, capacity, active) "
                    + "VALUES(?,?,?,?,?,TRUE)";

    public boolean ensureSlotsForDate(int officeId, LocalDate appointmentDate) {
        if (officeId <= 0 || appointmentDate == null || appointmentDate.isBefore(LocalDate.now())
                || isWeekend(appointmentDate)) {
            return false;
        }

        Connection connection = null;

        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);

            if (!lockActiveOffice(connection, officeId)) {
                connection.rollback();
                return false;
            }

            Set<LocalTime> existingStartTimes = findExistingStandardSlotStartTimes(
                    connection, officeId, appointmentDate
            );
            insertMissingStandardSlots(connection, officeId, appointmentDate, existingStartTimes);

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

    public List<AppointmentSlot> findAvailableActiveSlots(int officeId, LocalDate appointmentDate) {
        List<AppointmentSlot> slots = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection()
        ) {
            expirePastAppointments(connection);

            try (PreparedStatement ps = connection.prepareStatement(SELECT_AVAILABLE_ACTIVE_SLOTS)) {
                ps.setInt(1, officeId);
                ps.setDate(2, Date.valueOf(appointmentDate));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        slots.add(mapSlot(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return slots;
    }

    public AppointmentSlot findById(int slotId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)
        ) {

            ps.setInt(1, slotId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSlot(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean hasAvailableCapacity(int slotId) {

        try (
                Connection connection = DBConnection.getConnection()
        ) {
            expirePastAppointments(connection);

            try (PreparedStatement ps = connection.prepareStatement(HAS_AVAILABLE_CAPACITY)) {
                ps.setInt(1, slotId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("active_bookings") < rs.getInt("capacity");
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public int countActiveBookingsForSlot(int slotId) {

        try (
                Connection connection = DBConnection.getConnection()
        ) {
            expirePastAppointments(connection);

            try (PreparedStatement ps = connection.prepareStatement(COUNT_ACTIVE_BOOKINGS)) {
                ps.setInt(1, slotId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private int expirePastAppointments(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(EXPIRE_PAST_ACTIVE_APPOINTMENTS)) {
            return ps.executeUpdate();
        }
    }

    private boolean lockActiveOffice(Connection connection, int officeId) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(LOCK_ACTIVE_OFFICE)) {
            ps.setInt(1, officeId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Set<LocalTime> findExistingStandardSlotStartTimes(
            Connection connection, int officeId, LocalDate appointmentDate) throws SQLException {

        Set<LocalTime> existingStartTimes = new HashSet<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_STANDARD_SLOT_START_TIMES)) {
            ps.setInt(1, officeId);
            ps.setDate(2, Date.valueOf(appointmentDate));
            for (int i = 0; i < STANDARD_SLOT_TIMES.length; i++) {
                ps.setTime(i + 3, java.sql.Time.valueOf(STANDARD_SLOT_TIMES[i][0]));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existingStartTimes.add(rs.getTime("start_time").toLocalTime());
                }
            }
        }

        return existingStartTimes;
    }

    private void insertMissingStandardSlots(
            Connection connection, int officeId, LocalDate appointmentDate, Set<LocalTime> existingStartTimes)
            throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(INSERT_SLOT)) {
            for (LocalTime[] slotTime : STANDARD_SLOT_TIMES) {
                LocalTime startTime = slotTime[0];
                if (existingStartTimes.contains(startTime)) {
                    continue;
                }

                ps.setInt(1, officeId);
                ps.setDate(2, Date.valueOf(appointmentDate));
                ps.setTime(3, java.sql.Time.valueOf(startTime));
                ps.setTime(4, java.sql.Time.valueOf(slotTime[1]));
                ps.setInt(5, DEFAULT_SLOT_CAPACITY);
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    private boolean isWeekend(LocalDate appointmentDate) {
        switch (appointmentDate.getDayOfWeek()) {
            case SATURDAY:
            case SUNDAY:
                return true;
            default:
                return false;
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

    private AppointmentSlot mapSlot(ResultSet rs) throws SQLException {
        AppointmentSlot slot = new AppointmentSlot();
        slot.setId(rs.getInt("id"));
        slot.setOfficeId(rs.getInt("office_id"));
        slot.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
        slot.setStartTime(rs.getTime("start_time").toLocalTime());
        slot.setEndTime(rs.getTime("end_time").toLocalTime());
        slot.setCapacity(rs.getInt("capacity"));
        slot.setActive(rs.getBoolean("active"));
        slot.setCreatedAt(rs.getTimestamp("created_at"));
        return slot;
    }
}
