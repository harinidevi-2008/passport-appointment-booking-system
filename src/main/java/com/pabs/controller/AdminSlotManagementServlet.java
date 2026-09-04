package com.pabs.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pabs.dao.AppointmentSlotDAO;
import com.pabs.dao.AppointmentSlotDAO.CapacityUpdateResult;
import com.pabs.dao.AppointmentSlotDAO.CreateSlotResult;
import com.pabs.dao.PassportOfficeDAO;
import com.pabs.model.AppointmentSlot;
import com.pabs.model.PassportOffice;
import com.pabs.service.AppointmentNotificationService;
import com.pabs.service.AppointmentRecommendationService;
import com.pabs.service.AppointmentRecommendationService.OfficeDayMetrics;
import com.pabs.service.AppointmentRecommendationService.SlotRecommendation;
import com.pabs.util.CsrfUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/admin-slots")
public class AdminSlotManagementServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final PassportOfficeDAO officeDAO = new PassportOfficeDAO();
    private final AppointmentSlotDAO slotDAO = new AppointmentSlotDAO();
    private final AppointmentRecommendationService recommendationService = new AppointmentRecommendationService();
    private final AppointmentNotificationService appointmentNotificationService = new AppointmentNotificationService();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isAuthenticated(session)) {
            response.sendRedirect("login.jsp");
            return;
        }

        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        appointmentNotificationService.expirePastAppointmentsAndNotifyNoShow();
        showSlots(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException, ServletException {

        HttpSession session = request.getSession(false);
        if (!isAuthenticated(session)) {
            response.sendRedirect("login.jsp");
            return;
        }

        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!CsrfUtil.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = clean(request.getParameter("action"));
        String officeId = clean(request.getParameter("officeId"));
        String date = clean(request.getParameter("date"));

        if ("updateCapacity".equals(action)) {
            updateCapacity(request, response, officeId, date);
            return;
        }

        if ("createSlot".equals(action)) {
            createSlot(request, response, officeId, date);
            return;
        }

        Integer slotId = parseInt(request.getParameter("slotId"));
        if (slotId == null || !("activate".equals(action) || "deactivate".equals(action))) {
            redirectWithMessage(response, officeId, date, false, "Invalid slot action.");
            return;
        }

        AppointmentSlot slot = slotDAO.findById(slotId);
        if (slot == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean active = "activate".equals(action);
        boolean updated = slotDAO.updateActive(slotId, active);
        redirectWithMessage(response,
                String.valueOf(slot.getOfficeId()),
                String.valueOf(slot.getAppointmentDate()),
                updated,
                updated
                        ? "Slot " + (active ? "activated" : "deactivated") + " successfully."
                        : "Unable to update slot status.");
    }

    private void updateCapacity(HttpServletRequest request,
                                HttpServletResponse response,
                                String officeId,
                                String date)
            throws IOException {

        Integer slotId = parseInt(request.getParameter("slotId"));
        Integer newCapacity = parseInt(request.getParameter("capacity"));
        if (slotId == null || newCapacity == null) {
            redirectWithMessage(response, officeId, date, false, "Invalid slot capacity.");
            return;
        }

        AppointmentSlot slot = slotDAO.findById(slotId);
        if (slot == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        CapacityUpdateResult result = slotDAO.updateCapacitySafely(slotId, newCapacity);
        redirectWithMessage(response,
                String.valueOf(slot.getOfficeId()),
                String.valueOf(slot.getAppointmentDate()),
                result.isSuccess(),
                result.getMessage());
    }

    private void createSlot(HttpServletRequest request,
                            HttpServletResponse response,
                            String officeId,
                            String date)
            throws IOException {

        Integer parsedOfficeId = parseInt(officeId);
        LocalDate appointmentDate = parseDate(date);
        LocalTime startTime = parseTime(request.getParameter("startTime"));
        LocalTime endTime = parseTime(request.getParameter("endTime"));
        Integer capacity = parseInt(request.getParameter("capacity"));

        if (parsedOfficeId == null || appointmentDate == null || startTime == null
                || endTime == null || capacity == null) {
            redirectWithMessage(response, officeId, date, false,
                    "Please enter a valid date, time range, and capacity.");
            return;
        }

        CreateSlotResult result = slotDAO.createAdminSlot(
                parsedOfficeId, appointmentDate, startTime, endTime, capacity);
        redirectWithMessage(response, officeId, date, result.isSuccess(), result.getMessage());
    }

    private void showSlots(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {

        Integer officeId = parseInt(request.getParameter("officeId"));
        LocalDate date = parseDate(request.getParameter("date"));
        if (date == null) {
            date = LocalDate.now();
        }

        List<SlotManagementView> slotViews = new ArrayList<>();
        PassportOffice selectedOffice = null;
        if (officeId != null) {
            selectedOffice = officeDAO.findById(officeId);
            if (selectedOffice != null) {
                slotDAO.ensureSlotsForDate(officeId, date);
                OfficeDayMetrics officeDayMetrics = recommendationService.loadOfficeDayMetrics(officeId, date);
                request.setAttribute("officeDayMetrics", officeDayMetrics);
                List<AppointmentSlot> slots = slotDAO.findByOfficeAndDate(officeId, date);
                Map<Integer, Integer> bookingCounts = slotDAO.countActiveBookingsForSlots(slots);
                for (AppointmentSlot slot : slots) {
                    int bookedCount = bookingCounts.getOrDefault(slot.getId(), 0);
                    SlotRecommendation metrics = recommendationService.buildSlotRecommendation(slot, bookedCount);
                    slotViews.add(new SlotManagementView(slot, metrics));
                }
            }
        }

        request.setAttribute("offices", officeDAO.findActiveOffices());
        request.setAttribute("selectedOffice", selectedOffice);
        request.setAttribute("selectedOfficeId", officeId);
        request.setAttribute("selectedDate", date);
        request.setAttribute("slotViews", slotViews);
        request.getRequestDispatcher("admin-slots.jsp").forward(request, response);
    }

    private boolean isAuthenticated(HttpSession session) {
        return session != null && session.getAttribute("userId") != null;
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equalsIgnoreCase((String) session.getAttribute("role"));
    }

    private Integer parseInt(String value) {
        try {
            return isBlank(value) ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return isBlank(value) ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return isBlank(value) ? null : LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void redirectWithMessage(HttpServletResponse response,
                                     String officeId,
                                     String date,
                                     boolean success,
                                     String message)
            throws IOException {

        String redirectUrl = "admin-slots?officeId=" + encode(officeId)
                + "&date=" + encode(date)
                + "&status=" + (success ? "success" : "error")
                + "&message=" + encode(message);
        response.sendRedirect(redirectUrl);
    }

    private String encode(String value) {
        return URLEncoder.encode(clean(value), java.nio.charset.StandardCharsets.UTF_8);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class SlotManagementView {
        private final AppointmentSlot slot;
        private final SlotRecommendation metrics;

        public SlotManagementView(AppointmentSlot slot, SlotRecommendation metrics) {
            this.slot = slot;
            this.metrics = metrics;
        }

        public AppointmentSlot getSlot() {
            return slot;
        }

        public int getBookedCount() {
            return metrics.getBookedCount();
        }

        public int getRemainingCapacity() {
            return metrics.getRemainingCapacity();
        }

        public int getUtilizationPercent() {
            return metrics.getUtilizationPercent();
        }

        public String getCrowdLevel() {
            return metrics.getCrowdLevel();
        }
    }
}
