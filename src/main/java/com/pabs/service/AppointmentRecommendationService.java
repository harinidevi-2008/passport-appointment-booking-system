package com.pabs.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pabs.model.AppointmentSlot;
import com.pabs.model.PassportApplication;
import com.pabs.model.PassportOffice;
import com.pabs.dao.AppointmentSlotDAO;
import com.pabs.dao.AppointmentSlotDAO.OfficeDayCapacity;
import com.pabs.util.DBConnection;

public class AppointmentRecommendationService {

    private static final String CAPACITY_APPOINTMENT_STATUSES = "'BOOKED', 'RESCHEDULED', 'ATTENDED'";
    private static final String OCCUPIED_APPOINTMENT_STATUSES = "'BOOKED', 'RESCHEDULED', 'ATTENDED', 'COMPLETED'";
    private static final int OFFICE_LOOKAHEAD_DAYS = 14;
    private static final double SAME_CITY_DISTANCE_KM = 8.0;
    private static final double SAME_STATE_DISTANCE_KM = 75.0;
    private static final double UNKNOWN_DISTANCE_KM = 120.0;
    private static final Map<String, CityCoordinate> CITY_COORDINATES = cityCoordinates();

    private static final String OFFICE_SLOT_METRICS =
            "SELECT s.appointment_date, "
                    + "SUM(s.capacity) AS daily_capacity, "
                    + "SUM(COALESCE(occupied_counts.occupied_bookings, 0)) AS occupied_count, "
                    + "SUM(CASE WHEN TIMESTAMP(s.appointment_date, s.start_time) > NOW() "
                    + "THEN GREATEST(s.capacity - COALESCE(active_counts.active_bookings, 0), 0) ELSE 0 END) AS remaining_capacity, "
                    + "MIN(CASE WHEN TIMESTAMP(s.appointment_date, s.start_time) > NOW() "
                    + "AND GREATEST(s.capacity - COALESCE(active_counts.active_bookings, 0), 0) > 0 "
                    + "THEN s.start_time ELSE NULL END) AS earliest_start_time "
                    + "FROM appointment_slots s "
                    + "LEFT JOIN (SELECT slot_id, COUNT(*) AS occupied_bookings FROM appointments "
                    + "WHERE status IN (" + OCCUPIED_APPOINTMENT_STATUSES + ") GROUP BY slot_id) occupied_counts "
                    + "ON occupied_counts.slot_id=s.id "
                    + "LEFT JOIN (SELECT slot_id, COUNT(*) AS active_bookings FROM appointments "
                    + "WHERE status IN (" + CAPACITY_APPOINTMENT_STATUSES + ") GROUP BY slot_id) active_counts "
                    + "ON active_counts.slot_id=s.id "
                    + "WHERE s.office_id=? AND s.appointment_date BETWEEN ? AND ? AND s.active=TRUE "
                    + "GROUP BY s.appointment_date "
                    + "ORDER BY s.appointment_date";

    private final AppointmentSlotDAO slotDAO = new AppointmentSlotDAO();

    public List<SlotRecommendation> rankSlots(List<AppointmentSlot> slots, SlotBookingCounter counter) {
        List<SlotRecommendation> recommendations = new ArrayList<>();
        if (slots == null) {
            return recommendations;
        }

        for (AppointmentSlot slot : slots) {
            int bookedCount = counter.countActiveBookings(slot.getId());
            SlotRecommendation recommendation = buildSlotRecommendation(slot, bookedCount);
            if (recommendation.getRemainingCapacity() > 0 && slot.isActive()) {
                recommendations.add(recommendation);
            }
        }

        recommendations.sort(Comparator
                .comparingDouble(SlotRecommendation::getRecommendationScore)
                .thenComparing(item -> item.getSlot().getAppointmentDate())
                .thenComparing(item -> item.getSlot().getStartTime()));

        for (int i = 0; i < recommendations.size(); i++) {
            recommendations.get(i).setRecommended(i == 0);
        }

        return recommendations;
    }

    public List<OfficeRecommendation> recommendOffices(PassportApplication application,
                                                       List<PassportOffice> offices,
                                                       LocalDate fromDate) {
        List<OfficeRecommendation> recommendations = new ArrayList<>();
        if (application == null || offices == null || fromDate == null) {
            return recommendations;
        }

        LocalDate endDate = fromDate.plusDays(OFFICE_LOOKAHEAD_DAYS);
        for (PassportOffice office : offices) {
            OfficeSlotSummary summary = loadOfficeSlotSummary(office.getId(), fromDate, endDate);
            if (summary.availableSlots <= 0) {
                continue;
            }

            double distanceKm = distanceKm(application, office);
            String crowdLevel = crowdLevel(summary.utilizationPercent);
            double availabilityFactor = Math.max(0, 10 - summary.availableSlots) * 2.0;
            double daysUntilEarliest = ChronoUnit.DAYS.between(fromDate, summary.earliestDate);
            double score = distanceKm + (summary.utilizationPercent * 1.2)
                    + availabilityFactor + (daysUntilEarliest * 4.0);

            OfficeRecommendation recommendation = new OfficeRecommendation();
            recommendation.setOffice(office);
            recommendation.setDistanceKm(distanceKm);
            recommendation.setAvailableSlots(summary.availableSlots);
            recommendation.setDailyCapacity(summary.dailyCapacity);
            recommendation.setDailyOccupied(summary.dailyOccupied);
            recommendation.setAverageUtilizationPercent(summary.utilizationPercent);
            recommendation.setCrowdLevel(crowdLevel);
            recommendation.setEarliestDate(summary.earliestDate);
            recommendation.setEarliestStartTime(summary.earliestStartTime);
            recommendation.setRecommendationScore(score);
            recommendations.add(recommendation);
        }

        recommendations.sort(Comparator
                .comparingDouble(OfficeRecommendation::getRecommendationScore)
                .thenComparing(item -> item.getOffice().getOfficeName()));

        for (int i = 0; i < recommendations.size(); i++) {
            recommendations.get(i).setRecommended(i == 0);
        }

        return recommendations;
    }

    public SlotRecommendation buildSlotRecommendation(AppointmentSlot slot, int bookedCount) {
        int capacity = Math.max(slot.getCapacity(), 0);
        int remaining = Math.max(capacity - bookedCount, 0);
        int utilization = capacity == 0 ? 100 : (int) Math.round((bookedCount * 100.0) / capacity);
        SlotRecommendation recommendation = new SlotRecommendation();
        recommendation.setSlot(slot);
        recommendation.setBookedCount(bookedCount);
        recommendation.setRemainingCapacity(remaining);
        recommendation.setUtilizationPercent(Math.min(utilization, 100));
        recommendation.setCrowdLevel(crowdLevel(recommendation.getUtilizationPercent()));
        recommendation.setReason(slotRecommendationReason(recommendation));
        recommendation.setRecommendationScore(utilization - (remaining * 2.0));
        return recommendation;
    }

    public OfficeDayMetrics loadOfficeDayMetrics(int officeId, LocalDate appointmentDate) {
        OfficeDayCapacity capacity = slotDAO.loadOfficeDayCapacity(officeId, appointmentDate);
        int dailyCapacity = Math.max(capacity.getDailyCapacity(), 0);
        int occupied = Math.max(capacity.getOccupiedCount(), 0);
        int utilization = dailyCapacity == 0 ? 0 : (int) Math.round((occupied * 100.0) / dailyCapacity);
        OfficeDayMetrics metrics = new OfficeDayMetrics();
        metrics.setDailyCapacity(dailyCapacity);
        metrics.setDailyOccupied(occupied);
        metrics.setUtilizationPercent(Math.min(utilization, 100));
        metrics.setCrowdLevel(crowdLevel(utilization));
        return metrics;
    }

    private OfficeSlotSummary loadOfficeSlotSummary(int officeId, LocalDate fromDate, LocalDate endDate) {
        OfficeSlotSummary summary = new OfficeSlotSummary();
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(OFFICE_SLOT_METRICS)
        ) {
            ps.setInt(1, officeId);
            ps.setDate(2, Date.valueOf(fromDate));
            ps.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = ps.executeQuery()) {
                int availableCapacity = 0;
                while (rs.next()) {
                    int remainingCapacity = rs.getInt("remaining_capacity");
                    if (remainingCapacity <= 0) {
                        continue;
                    }

                    int capacity = rs.getInt("daily_capacity");
                    int occupied = rs.getInt("occupied_count");
                    int utilization = capacity == 0
                            ? 100
                            : (int) Math.round(occupied * 100.0 / capacity);
                    LocalTime startTime = rs.getTime("earliest_start_time") == null
                            ? null
                            : rs.getTime("earliest_start_time").toLocalTime();
                    if (startTime == null) {
                        continue;
                    }

                    availableCapacity += remainingCapacity;

                    LocalDate date = rs.getDate("appointment_date").toLocalDate();
                    if (summary.earliestDate == null
                            || LocalDateTime.of(date, startTime).isBefore(
                                    LocalDateTime.of(summary.earliestDate, summary.earliestStartTime))) {
                        summary.earliestDate = date;
                        summary.earliestStartTime = startTime;
                        summary.dailyCapacity = Math.max(capacity, 0);
                        summary.dailyOccupied = Math.max(occupied, 0);
                        summary.utilizationPercent = Math.min(Math.max(utilization, 0), 100);
                    }
                }
                summary.availableSlots = availableCapacity;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }

    private String crowdLevel(int utilizationPercent) {
        if (utilizationPercent <= 40) {
            return "LOW";
        }
        if (utilizationPercent <= 70) {
            return "MODERATE";
        }
        return "HIGH";
    }

    private String slotRecommendationReason(SlotRecommendation recommendation) {
        if (recommendation.getRemainingCapacity() <= 0) {
            return "No remaining slot capacity.";
        }
        if ("LOW".equals(recommendation.getCrowdLevel())) {
            return "Low slot crowd and good remaining capacity.";
        }
        if ("MODERATE".equals(recommendation.getCrowdLevel())) {
            return "Moderate slot crowd with remaining capacity.";
        }
        return "Remaining capacity is available, but this slot is busier than lower-crowd options.";
    }

    private double distanceKm(PassportApplication application, PassportOffice office) {
        double[] applicationCoordinates = approximateCoordinates(application.getCity(), application.getState());
        double[] officeCoordinates = officeCoordinates(office);
        if (applicationCoordinates != null && officeCoordinates != null) {
            return haversine(applicationCoordinates[0], applicationCoordinates[1],
                    officeCoordinates[0], officeCoordinates[1]);
        }

        if (equalsIgnoreCase(application.getCity(), office.getCity())
                && equalsIgnoreCase(application.getState(), office.getState())) {
            return SAME_CITY_DISTANCE_KM;
        }
        if (equalsIgnoreCase(application.getState(), office.getState())) {
            return SAME_STATE_DISTANCE_KM;
        }
        return UNKNOWN_DISTANCE_KM;
    }

    private double[] officeCoordinates(PassportOffice office) {
        if (office.getLatitude() != null && office.getLongitude() != null) {
            return new double[] {office.getLatitude().doubleValue(), office.getLongitude().doubleValue()};
        }
        return approximateCoordinates(office.getCity(), office.getState());
    }

    private double[] approximateCoordinates(String city, String state) {
        CityCoordinate coordinate = CITY_COORDINATES.get(locationKey(city, state));
        if (coordinate == null) {
            return null;
        }
        return new double[] {coordinate.latitude(), coordinate.longitude()};
    }

    private boolean equalsIgnoreCase(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double radiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radiusKm * c;
    }

    private static Map<String, CityCoordinate> cityCoordinates() {
        Map<String, CityCoordinate> coordinates = new HashMap<>();
        putCity(coordinates, "Arani", "Tamil Nadu", 12.6699, 79.2856);
        putCity(coordinates, "Bodinayakkanur", "Tamil Nadu", 10.0116, 77.3498);
        putCity(coordinates, "Chennai", "Tamil Nadu", 13.0827, 80.2707);
        putCity(coordinates, "Chidambaram", "Tamil Nadu", 11.3990, 79.6954);
        putCity(coordinates, "Coimbatore", "Tamil Nadu", 11.0168, 76.9558);
        putCity(coordinates, "Coonoor", "Tamil Nadu", 11.3530, 76.7959);
        putCity(coordinates, "Cuddalore", "Tamil Nadu", 11.7447, 79.7680);
        putCity(coordinates, "Devakottai", "Tamil Nadu", 9.9470, 78.8233);
        putCity(coordinates, "Dharmapuri", "Tamil Nadu", 12.1277, 78.1579);
        putCity(coordinates, "Erode", "Tamil Nadu", 11.3410, 77.7172);
        putCity(coordinates, "Kallakurichi", "Tamil Nadu", 11.7404, 78.9590);
        putCity(coordinates, "Kancheepuram", "Tamil Nadu", 12.8342, 79.7036);
        putCity(coordinates, "Kodai Road", "Tamil Nadu", 10.1817, 77.9767);
        putCity(coordinates, "Krishnagiri", "Tamil Nadu", 12.5186, 78.2137);
        putCity(coordinates, "Madurai", "Tamil Nadu", 9.9252, 78.1198);
        putCity(coordinates, "Nagercoil", "Tamil Nadu", 8.1833, 77.4119);
        putCity(coordinates, "Rajapalayam", "Tamil Nadu", 9.4524, 77.5530);
        putCity(coordinates, "Ramanathapuram", "Tamil Nadu", 9.3639, 78.8395);
        putCity(coordinates, "Ranipet", "Tamil Nadu", 12.9249, 79.3333);
        putCity(coordinates, "Rasipuram", "Tamil Nadu", 11.4600, 78.1864);
        putCity(coordinates, "Salem", "Tamil Nadu", 11.6643, 78.1460);
        putCity(coordinates, "Tambaram", "Tamil Nadu", 12.9249, 80.1000);
        putCity(coordinates, "Thanjavur", "Tamil Nadu", 10.7870, 79.1378);
        putCity(coordinates, "Thoothukkudi", "Tamil Nadu", 8.7642, 78.1348);
        putCity(coordinates, "Tiruchirappalli", "Tamil Nadu", 10.7905, 78.7047);
        putCity(coordinates, "Tirunelveli", "Tamil Nadu", 8.7139, 77.7567);
        putCity(coordinates, "Tiruvallur", "Tamil Nadu", 13.1439, 79.9089);
        putCity(coordinates, "Tiruvannamalai", "Tamil Nadu", 12.2253, 79.0747);
        putCity(coordinates, "Vellore", "Tamil Nadu", 12.9165, 79.1325);
        putCity(coordinates, "Villupuram", "Tamil Nadu", 11.9401, 79.4861);
        putCity(coordinates, "Virudhunagar", "Tamil Nadu", 9.5680, 77.9624);
        return Map.copyOf(coordinates);
    }

    private static void putCity(Map<String, CityCoordinate> coordinates,
                                String city,
                                String state,
                                double latitude,
                                double longitude) {
        coordinates.put(locationKey(city, state), new CityCoordinate(latitude, longitude));
    }

    private static String locationKey(String city, String state) {
        return (city == null ? "" : city.trim().toLowerCase())
                + "|" + (state == null ? "" : state.trim().toLowerCase());
    }

    private static class OfficeSlotSummary {
        private int availableSlots;
        private int dailyCapacity;
        private int dailyOccupied;
        private int utilizationPercent;
        private LocalDate earliestDate;
        private LocalTime earliestStartTime;
    }

    private record CityCoordinate(double latitude, double longitude) {
    }

    public static class OfficeDayMetrics {
        private int dailyCapacity;
        private int dailyOccupied;
        private int utilizationPercent;
        private String crowdLevel;

        public int getDailyCapacity() {
            return dailyCapacity;
        }

        public void setDailyCapacity(int dailyCapacity) {
            this.dailyCapacity = dailyCapacity;
        }

        public int getDailyOccupied() {
            return dailyOccupied;
        }

        public void setDailyOccupied(int dailyOccupied) {
            this.dailyOccupied = dailyOccupied;
        }

        public int getUtilizationPercent() {
            return utilizationPercent;
        }

        public void setUtilizationPercent(int utilizationPercent) {
            this.utilizationPercent = utilizationPercent;
        }

        public String getCrowdLevel() {
            return crowdLevel;
        }

        public void setCrowdLevel(String crowdLevel) {
            this.crowdLevel = crowdLevel;
        }
    }

    public interface SlotBookingCounter {
        int countActiveBookings(int slotId);
    }

    public static class SlotRecommendation {
        private AppointmentSlot slot;
        private int bookedCount;
        private int remainingCapacity;
        private int utilizationPercent;
        private String crowdLevel;
        private String reason;
        private double recommendationScore;
        private boolean recommended;

        public AppointmentSlot getSlot() {
            return slot;
        }

        public void setSlot(AppointmentSlot slot) {
            this.slot = slot;
        }

        public int getBookedCount() {
            return bookedCount;
        }

        public void setBookedCount(int bookedCount) {
            this.bookedCount = bookedCount;
        }

        public int getRemainingCapacity() {
            return remainingCapacity;
        }

        public void setRemainingCapacity(int remainingCapacity) {
            this.remainingCapacity = remainingCapacity;
        }

        public int getUtilizationPercent() {
            return utilizationPercent;
        }

        public void setUtilizationPercent(int utilizationPercent) {
            this.utilizationPercent = utilizationPercent;
        }

        public String getCrowdLevel() {
            return crowdLevel;
        }

        public void setCrowdLevel(String crowdLevel) {
            this.crowdLevel = crowdLevel;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public double getRecommendationScore() {
            return recommendationScore;
        }

        public void setRecommendationScore(double recommendationScore) {
            this.recommendationScore = recommendationScore;
        }

        public boolean isRecommended() {
            return recommended;
        }

        public void setRecommended(boolean recommended) {
            this.recommended = recommended;
        }
    }

    public static class OfficeRecommendation {
        private PassportOffice office;
        private double distanceKm;
        private int availableSlots;
        private int dailyCapacity;
        private int dailyOccupied;
        private int averageUtilizationPercent;
        private String crowdLevel;
        private LocalDate earliestDate;
        private LocalTime earliestStartTime;
        private double recommendationScore;
        private boolean recommended;

        public PassportOffice getOffice() {
            return office;
        }

        public void setOffice(PassportOffice office) {
            this.office = office;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(double distanceKm) {
            this.distanceKm = distanceKm;
        }

        public int getAvailableSlots() {
            return availableSlots;
        }

        public void setAvailableSlots(int availableSlots) {
            this.availableSlots = availableSlots;
        }

        public int getAverageUtilizationPercent() {
            return averageUtilizationPercent;
        }

        public void setAverageUtilizationPercent(int averageUtilizationPercent) {
            this.averageUtilizationPercent = averageUtilizationPercent;
        }

        public int getDailyCapacity() {
            return dailyCapacity;
        }

        public void setDailyCapacity(int dailyCapacity) {
            this.dailyCapacity = dailyCapacity;
        }

        public int getDailyOccupied() {
            return dailyOccupied;
        }

        public void setDailyOccupied(int dailyOccupied) {
            this.dailyOccupied = dailyOccupied;
        }

        public String getCrowdLevel() {
            return crowdLevel;
        }

        public void setCrowdLevel(String crowdLevel) {
            this.crowdLevel = crowdLevel;
        }

        public LocalDate getEarliestDate() {
            return earliestDate;
        }

        public void setEarliestDate(LocalDate earliestDate) {
            this.earliestDate = earliestDate;
        }

        public LocalTime getEarliestStartTime() {
            return earliestStartTime;
        }

        public void setEarliestStartTime(LocalTime earliestStartTime) {
            this.earliestStartTime = earliestStartTime;
        }

        public double getRecommendationScore() {
            return recommendationScore;
        }

        public void setRecommendationScore(double recommendationScore) {
            this.recommendationScore = recommendationScore;
        }

        public boolean isRecommended() {
            return recommended;
        }

        public void setRecommended(boolean recommended) {
            this.recommended = recommended;
        }
    }
}
