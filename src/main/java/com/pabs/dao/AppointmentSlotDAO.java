package com.pabs.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pabs.model.AppointmentSlot;
import com.pabs.util.DBConnection;

public class AppointmentSlotDAO {

    private static final String ACTIVE_APPOINTMENT_STATUSES = "'BOOKED', 'RESCHEDULED'";
    private static final String CAPACITY_APPOINTMENT_STATUSES = "'BOOKED', 'RESCHEDULED', 'ATTENDED'";
    private static final String OCCUPIED_APPOINTMENT_STATUSES = "'BOOKED', 'RESCHEDULED', 'ATTENDED', 'COMPLETED'";
    private static final String EXPIRE_PAST_ACTIVE_APPOINTMENTS =
            "UPDATE appointments a JOIN appointment_slots s ON s.id=a.slot_id "
                    + "SET a.status='NO_SHOW' "
                    + "WHERE a.status IN (" + ACTIVE_APPOINTMENT_STATUSES + ") "
                    + "AND TIMESTAMP(s.appointment_date, s.end_time) < NOW()";
    private static final int DEFAULT_SLOT_CAPACITY = 3;

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
                    + "AND a.status IN (" + CAPACITY_APPOINTMENT_STATUSES + ") "
                    + "AND TIMESTAMP(s.appointment_date, s.end_time) >= NOW() "
                    + "WHERE s.office_id=? AND s.appointment_date=? AND s.active=TRUE "
                    + "GROUP BY s.id, s.office_id, s.appointment_date, s.start_time, s.end_time, "
                    + "s.capacity, s.active, s.created_at "
                    + "HAVING COUNT(a.id) < s.capacity "
                    + "ORDER BY s.start_time";

    private static final String SELECT_BY_ID =
            "SELECT " + SELECT_COLUMNS + " FROM appointment_slots WHERE id=?";

    private static final String SELECT_BY_OFFICE_AND_DATE =
            "SELECT " + SELECT_COLUMNS + " FROM appointment_slots "
                    + "WHERE office_id=? AND appointment_date=? ORDER BY start_time";

    private static final String UPDATE_ACTIVE =
            "UPDATE appointment_slots SET active=? WHERE id=?";

    private static final String LOCK_SLOT_FOR_CAPACITY_EDIT =
            "SELECT id, capacity FROM appointment_slots WHERE id=? FOR UPDATE";

    private static final String UPDATE_CAPACITY =
            "UPDATE appointment_slots SET capacity=? WHERE id=?";

    private static final String COUNT_ACTIVE_BOOKINGS =
            "SELECT COUNT(*) FROM appointments a JOIN appointment_slots s ON s.id=a.slot_id "
                    + "WHERE a.slot_id=? "
                    + "AND a.status IN (" + CAPACITY_APPOINTMENT_STATUSES + ") "
                    + "AND TIMESTAMP(s.appointment_date, s.end_time) >= NOW()";

    private static final String OFFICE_DAY_METRICS =
            "SELECT COALESCE(SUM(s.capacity), 0) AS daily_capacity, "
                    + "COALESCE(SUM(COALESCE(occupied_counts.occupied_bookings, 0)), 0) AS occupied_count "
                    + "FROM appointment_slots s "
                    + "LEFT JOIN (SELECT slot_id, COUNT(*) AS occupied_bookings FROM appointments "
                    + "WHERE status IN (" + OCCUPIED_APPOINTMENT_STATUSES + ") GROUP BY slot_id) occupied_counts "
                    + "ON occupied_counts.slot_id=s.id "
                    + "WHERE s.office_id=? AND s.appointment_date=? AND s.active=TRUE";

    private static final String HAS_AVAILABLE_CAPACITY =
            "SELECT s.capacity, COUNT(a.id) AS active_bookings "
                    + "FROM appointment_slots s "
                    + "LEFT JOIN appointments a ON a.slot_id=s.id "
                    + "AND a.status IN (" + CAPACITY_APPOINTMENT_STATUSES + ") "
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

    private static final String INSERT_ADMIN_SLOT =
            "INSERT INTO appointment_slots"
                    + "(office_id, appointment_date, start_time, end_time, capacity, active) "
                    + "VALUES(?,?,?,?,?,TRUE)";

    private static final String HAS_OVERLAPPING_SLOT =
            "SELECT id FROM appointment_slots "
                    + "WHERE office_id=? AND appointment_date=? "
                    + "AND NOT (end_time <= ? OR start_time >= ?) "
                    + "LIMIT 1";

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

    public List<AppointmentSlot> findByOfficeAndDate(int officeId, LocalDate appointmentDate) {
        List<AppointmentSlot> slots = new ArrayList<>();
        if (officeId <= 0 || appointmentDate == null) {
            return slots;
        }

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_BY_OFFICE_AND_DATE)
        ) {
            expirePastAppointments(connection);
            ps.setInt(1, officeId);
            ps.setDate(2, Date.valueOf(appointmentDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapSlot(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return slots;
    }

    public boolean updateActive(int slotId, boolean active) {
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(UPDATE_ACTIVE)
        ) {
            ps.setBoolean(1, active);
            ps.setInt(2, slotId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public CapacityUpdateResult updateCapacitySafely(int slotId, int newCapacity) {
        if (slotId <= 0 || newCapacity <= 0) {
            return CapacityUpdateResult.invalid("Invalid slot capacity.");
        }

        Connection connection = null;
        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);
            expirePastAppointments(connection);

            if (!lockSlotForCapacityEdit(connection, slotId)) {
                connection.rollback();
                return CapacityUpdateResult.notFound();
            }

            int bookedCount = countActiveBookingsForSlot(connection, slotId);
            if (newCapacity < bookedCount) {
                connection.rollback();
                return CapacityUpdateResult.invalid(
                        "Capacity cannot be lower than the current booked count.");
            }

            try (PreparedStatement ps = connection.prepareStatement(UPDATE_CAPACITY)) {
                ps.setInt(1, newCapacity);
                ps.setInt(2, slotId);
                if (ps.executeUpdate() != 1) {
                    connection.rollback();
                    return CapacityUpdateResult.failed();
                }
            }

            connection.commit();
            return CapacityUpdateResult.success();

        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }

        return CapacityUpdateResult.failed();
    }

    public CreateSlotResult createAdminSlot(int officeId,
                                            LocalDate appointmentDate,
                                            LocalTime startTime,
                                            LocalTime endTime,
                                            int capacity) {
        if (officeId <= 0 || appointmentDate == null || startTime == null || endTime == null
                || capacity <= 0 || !endTime.isAfter(startTime)
                || LocalDateTime.of(appointmentDate, startTime).isBefore(LocalDateTime.now())) {
            return CreateSlotResult.invalid("Please enter a valid future slot and positive capacity.");
        }

        Connection connection = null;
        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);

            if (!lockActiveOffice(connection, officeId)) {
                connection.rollback();
                return CreateSlotResult.invalid("Please select an active passport office.");
            }

            if (hasOverlappingSlot(connection, officeId, appointmentDate, startTime, endTime)) {
                connection.rollback();
                return CreateSlotResult.invalid("The new slot overlaps an existing slot for this office and date.");
            }

            try (PreparedStatement ps = connection.prepareStatement(INSERT_ADMIN_SLOT)) {
                ps.setInt(1, officeId);
                ps.setDate(2, Date.valueOf(appointmentDate));
                ps.setTime(3, java.sql.Time.valueOf(startTime));
                ps.setTime(4, java.sql.Time.valueOf(endTime));
                ps.setInt(5, capacity);
                ps.executeUpdate();
            }

            connection.commit();
            return CreateSlotResult.success();

        } catch (SQLIntegrityConstraintViolationException e) {
            rollbackQuietly(connection);
            return CreateSlotResult.invalid("A slot already exists for this office, date, and start time.");
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }

        return CreateSlotResult.failed();
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

    private int countActiveBookingsForSlot(Connection connection, int slotId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(COUNT_ACTIVE_BOOKINGS)) {
            ps.setInt(1, slotId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    public Map<Integer, Integer> countActiveBookingsForSlots(List<AppointmentSlot> slots) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (slots == null || slots.isEmpty()) {
            return counts;
        }

        List<Integer> slotIds = new ArrayList<>();
        for (AppointmentSlot slot : slots) {
            if (slot != null && slot.getId() > 0) {
                slotIds.add(slot.getId());
                counts.put(slot.getId(), 0);
            }
        }
        if (slotIds.isEmpty()) {
            return counts;
        }

        StringBuilder sql = new StringBuilder()
                .append("SELECT a.slot_id, COUNT(*) AS active_bookings ")
                .append("FROM appointments a JOIN appointment_slots s ON s.id=a.slot_id ")
                .append("WHERE a.status IN (").append(CAPACITY_APPOINTMENT_STATUSES).append(") ")
                .append("AND TIMESTAMP(s.appointment_date, s.end_time) >= NOW() ")
                .append("AND a.slot_id IN (");
        for (int i = 0; i < slotIds.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append('?');
        }
        sql.append(") GROUP BY a.slot_id");

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql.toString())
        ) {
            expirePastAppointments(connection);
            for (int i = 0; i < slotIds.size(); i++) {
                ps.setInt(i + 1, slotIds.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getInt("slot_id"), rs.getInt("active_bookings"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return counts;
    }

    public OfficeDayCapacity loadOfficeDayCapacity(int officeId, LocalDate appointmentDate) {
        if (officeId <= 0 || appointmentDate == null) {
            return new OfficeDayCapacity(0, 0);
        }

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(OFFICE_DAY_METRICS)
        ) {
            expirePastAppointments(connection);
            ps.setInt(1, officeId);
            ps.setDate(2, Date.valueOf(appointmentDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new OfficeDayCapacity(rs.getInt("daily_capacity"), rs.getInt("occupied_count"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new OfficeDayCapacity(0, 0);
    }

    private int expirePastAppointments(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(EXPIRE_PAST_ACTIVE_APPOINTMENTS)) {
            return ps.executeUpdate();
        }
    }

    private boolean hasOverlappingSlot(Connection connection,
                                       int officeId,
                                       LocalDate appointmentDate,
                                       LocalTime startTime,
                                       LocalTime endTime)
            throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(HAS_OVERLAPPING_SLOT)) {
            ps.setInt(1, officeId);
            ps.setDate(2, Date.valueOf(appointmentDate));
            ps.setTime(3, java.sql.Time.valueOf(startTime));
            ps.setTime(4, java.sql.Time.valueOf(endTime));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
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

    private boolean lockSlotForCapacityEdit(Connection connection, int slotId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(LOCK_SLOT_FOR_CAPACITY_EDIT)) {
            ps.setInt(1, slotId);
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

    public static class OfficeDayCapacity {
        private final int dailyCapacity;
        private final int occupiedCount;

        public OfficeDayCapacity(int dailyCapacity, int occupiedCount) {
            this.dailyCapacity = dailyCapacity;
            this.occupiedCount = occupiedCount;
        }

        public int getDailyCapacity() {
            return dailyCapacity;
        }

        public int getOccupiedCount() {
            return occupiedCount;
        }
    }

    public static class CapacityUpdateResult {
        private final boolean success;
        private final String message;

        private CapacityUpdateResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static CapacityUpdateResult success() {
            return new CapacityUpdateResult(true, "Slot capacity updated successfully.");
        }

        public static CapacityUpdateResult invalid(String message) {
            return new CapacityUpdateResult(false, message);
        }

        public static CapacityUpdateResult notFound() {
            return new CapacityUpdateResult(false, "Slot was not found.");
        }

        public static CapacityUpdateResult failed() {
            return new CapacityUpdateResult(false, "Unable to update slot capacity.");
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class CreateSlotResult {
        private final boolean success;
        private final String message;

        private CreateSlotResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static CreateSlotResult success() {
            return new CreateSlotResult(true, "Slot created successfully.");
        }

        public static CreateSlotResult invalid(String message) {
            return new CreateSlotResult(false, message);
        }

        public static CreateSlotResult failed() {
            return new CreateSlotResult(false, "Unable to create the slot.");
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
