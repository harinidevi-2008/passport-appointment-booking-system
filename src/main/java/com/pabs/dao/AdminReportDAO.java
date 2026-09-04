package com.pabs.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pabs.util.DBConnection;

public class AdminReportDAO {

    private static final String COUNT_CITIZENS =
            "SELECT COUNT(*) FROM users WHERE role='USER'";
    private static final String COUNT_APPLICATIONS =
            "SELECT COUNT(*) FROM passport_applications";
    private static final String COUNT_APPOINTMENTS =
            "SELECT COUNT(*) FROM appointments";
    private static final String COUNT_APPLICATIONS_BY_STATUS =
            "SELECT status, COUNT(*) AS total FROM passport_applications GROUP BY status ORDER BY status";
    private static final String COUNT_APPOINTMENTS_BY_STATUS =
            "SELECT status, COUNT(*) AS total FROM appointments GROUP BY status ORDER BY status";
    private static final String COUNT_APPLICATION_STATUS =
            "SELECT COUNT(*) FROM passport_applications WHERE status=?";
    private static final String COUNT_APPOINTMENT_STATUS =
            "SELECT COUNT(*) FROM appointments WHERE status=?";
    private static final String DAILY_APPOINTMENTS =
            "SELECT a.appointment_number, a.status AS appointment_status, "
                    + "pa.application_number, pa.full_name, po.office_name, "
                    + "s.appointment_date, s.start_time, s.end_time "
                    + "FROM appointments a "
                    + "JOIN passport_applications pa ON pa.id=a.application_id "
                    + "JOIN appointment_slots s ON s.id=a.slot_id "
                    + "JOIN passport_offices po ON po.id=s.office_id "
                    + "WHERE s.appointment_date=? "
                    + "ORDER BY s.start_time, po.office_name, pa.full_name";

    public int countCitizens() {
        return count(COUNT_CITIZENS);
    }

    public int countApplications() {
        return count(COUNT_APPLICATIONS);
    }

    public int countAppointments() {
        return count(COUNT_APPOINTMENTS);
    }

    public int countApplicationsByStatus(String status) {
        return countStatus(COUNT_APPLICATION_STATUS, status);
    }

    public int countAppointmentsByStatus(String status) {
        return countStatus(COUNT_APPOINTMENT_STATUS, status);
    }

    public Map<String, Integer> applicationStatusSummary() {
        return summary(COUNT_APPLICATIONS_BY_STATUS);
    }

    public Map<String, Integer> appointmentStatusSummary() {
        return summary(COUNT_APPOINTMENTS_BY_STATUS);
    }

    public List<DailyAppointmentReportRow> findDailyAppointments(LocalDate appointmentDate) {
        List<DailyAppointmentReportRow> rows = new ArrayList<>();
        if (appointmentDate == null) {
            return rows;
        }

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(DAILY_APPOINTMENTS)
        ) {
            ps.setDate(1, Date.valueOf(appointmentDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DailyAppointmentReportRow row = new DailyAppointmentReportRow();
                    row.setAppointmentNumber(rs.getString("appointment_number"));
                    row.setApplicationNumber(rs.getString("application_number"));
                    row.setCitizenName(rs.getString("full_name"));
                    row.setOfficeName(rs.getString("office_name"));
                    row.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
                    row.setStartTime(rs.getTime("start_time").toLocalTime());
                    row.setEndTime(rs.getTime("end_time").toLocalTime());
                    row.setAppointmentStatus(rs.getString("appointment_status"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    public AdminReportData loadReportData(LocalDate appointmentDate,
                                          String[] applicationStatuses,
                                          String[] appointmentStatuses) {
        AdminReportData data = new AdminReportData();
        data.setSelectedDate(appointmentDate);
        data.setApplicationSummary(fillKnownStatuses(applicationStatuses, applicationStatusSummary()));
        data.setAppointmentSummary(fillKnownStatuses(appointmentStatuses, appointmentStatusSummary()));
        data.setDailyAppointments(findDailyAppointments(appointmentDate));
        data.setTotalCitizens(countCitizens());
        data.setTotalApplications(countApplications());
        data.setVerifiedApplications(countApplicationsByStatus("VERIFIED"));
        data.setProcessingApplications(countApplicationsByStatus("PROCESSING"));
        data.setTotalAppointments(countAppointments());
        data.setCompletedAppointments(countAppointmentsByStatus("COMPLETED"));
        data.setCancelledAppointments(countAppointmentsByStatus("CANCELLED"));
        data.setNoShowAppointments(countAppointmentsByStatus("NO_SHOW"));
        return data;
    }

    private Map<String, Integer> fillKnownStatuses(String[] statuses, Map<String, Integer> actualCounts) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String status : statuses) {
            counts.put(status, actualCounts.getOrDefault(status, 0));
        }
        for (Map.Entry<String, Integer> entry : actualCounts.entrySet()) {
            counts.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return counts;
    }

    private int count(String sql) {
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int countStatus(String sql, String status) {
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)
        ) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Map<String, Integer> summary(String sql) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                counts.put(rs.getString("status"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts;
    }

    public static class DailyAppointmentReportRow {
        private String appointmentNumber;
        private String applicationNumber;
        private String citizenName;
        private String officeName;
        private LocalDate appointmentDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String appointmentStatus;

        public String getAppointmentNumber() {
            return appointmentNumber;
        }

        public void setAppointmentNumber(String appointmentNumber) {
            this.appointmentNumber = appointmentNumber;
        }

        public String getApplicationNumber() {
            return applicationNumber;
        }

        public void setApplicationNumber(String applicationNumber) {
            this.applicationNumber = applicationNumber;
        }

        public String getCitizenName() {
            return citizenName;
        }

        public void setCitizenName(String citizenName) {
            this.citizenName = citizenName;
        }

        public String getOfficeName() {
            return officeName;
        }

        public void setOfficeName(String officeName) {
            this.officeName = officeName;
        }

        public LocalDate getAppointmentDate() {
            return appointmentDate;
        }

        public void setAppointmentDate(LocalDate appointmentDate) {
            this.appointmentDate = appointmentDate;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }

        public String getAppointmentStatus() {
            return appointmentStatus;
        }

        public void setAppointmentStatus(String appointmentStatus) {
            this.appointmentStatus = appointmentStatus;
        }
    }

    public static class AdminReportData {
        private LocalDate selectedDate;
        private Map<String, Integer> applicationSummary;
        private Map<String, Integer> appointmentSummary;
        private List<DailyAppointmentReportRow> dailyAppointments;
        private int totalCitizens;
        private int totalApplications;
        private int verifiedApplications;
        private int processingApplications;
        private int totalAppointments;
        private int completedAppointments;
        private int cancelledAppointments;
        private int noShowAppointments;

        public LocalDate getSelectedDate() {
            return selectedDate;
        }

        public void setSelectedDate(LocalDate selectedDate) {
            this.selectedDate = selectedDate;
        }

        public Map<String, Integer> getApplicationSummary() {
            return applicationSummary;
        }

        public void setApplicationSummary(Map<String, Integer> applicationSummary) {
            this.applicationSummary = applicationSummary;
        }

        public Map<String, Integer> getAppointmentSummary() {
            return appointmentSummary;
        }

        public void setAppointmentSummary(Map<String, Integer> appointmentSummary) {
            this.appointmentSummary = appointmentSummary;
        }

        public List<DailyAppointmentReportRow> getDailyAppointments() {
            return dailyAppointments;
        }

        public void setDailyAppointments(List<DailyAppointmentReportRow> dailyAppointments) {
            this.dailyAppointments = dailyAppointments;
        }

        public int getTotalCitizens() { return totalCitizens; }
        public void setTotalCitizens(int totalCitizens) { this.totalCitizens = totalCitizens; }
        public int getTotalApplications() { return totalApplications; }
        public void setTotalApplications(int totalApplications) { this.totalApplications = totalApplications; }
        public int getVerifiedApplications() { return verifiedApplications; }
        public void setVerifiedApplications(int verifiedApplications) { this.verifiedApplications = verifiedApplications; }
        public int getProcessingApplications() { return processingApplications; }
        public void setProcessingApplications(int processingApplications) { this.processingApplications = processingApplications; }
        public int getTotalAppointments() { return totalAppointments; }
        public void setTotalAppointments(int totalAppointments) { this.totalAppointments = totalAppointments; }
        public int getCompletedAppointments() { return completedAppointments; }
        public void setCompletedAppointments(int completedAppointments) { this.completedAppointments = completedAppointments; }
        public int getCancelledAppointments() { return cancelledAppointments; }
        public void setCancelledAppointments(int cancelledAppointments) { this.cancelledAppointments = cancelledAppointments; }
        public int getNoShowAppointments() { return noShowAppointments; }
        public void setNoShowAppointments(int noShowAppointments) { this.noShowAppointments = noShowAppointments; }
    }
}
