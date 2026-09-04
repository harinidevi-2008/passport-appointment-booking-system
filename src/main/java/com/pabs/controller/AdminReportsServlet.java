package com.pabs.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.pabs.dao.AdminReportDAO;
import com.pabs.dao.AdminReportDAO.AdminReportData;
import com.pabs.service.AppointmentNotificationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/admin-reports")
public class AdminReportsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String[] APPLICATION_STATUSES = {
            "SUBMITTED", "UNDER_REVIEW", "VERIFIED", "PROCESSING", "APPROVED",
            "PRINTING", "DISPATCHED", "DELIVERED", "REJECTED", "CANCELLED"
    };
    public static final String[] APPOINTMENT_STATUSES = {
            "BOOKED", "RESCHEDULED", "ATTENDED", "COMPLETED", "CANCELLED", "NO_SHOW"
    };

    private final AdminReportDAO reportDAO = new AdminReportDAO();
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

        LocalDate selectedDate = parseDate(request.getParameter("date"));
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }

        AdminReportData reportData = reportDAO.loadReportData(
                selectedDate, APPLICATION_STATUSES, APPOINTMENT_STATUSES);

        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("applicationSummary", reportData.getApplicationSummary());
        request.setAttribute("appointmentSummary", reportData.getAppointmentSummary());
        request.setAttribute("dailyAppointments", reportData.getDailyAppointments());
        request.setAttribute("totalCitizens", reportData.getTotalCitizens());
        request.setAttribute("totalApplications", reportData.getTotalApplications());
        request.setAttribute("verifiedApplications", reportData.getVerifiedApplications());
        request.setAttribute("processingApplications", reportData.getProcessingApplications());
        request.setAttribute("totalAppointments", reportData.getTotalAppointments());
        request.setAttribute("completedAppointments", reportData.getCompletedAppointments());
        request.setAttribute("cancelledAppointments", reportData.getCancelledAppointments());
        request.setAttribute("noShowAppointments", reportData.getNoShowAppointments());
        request.getRequestDispatcher("admin-reports.jsp").forward(request, response);
    }

    private LocalDate parseDate(String value) {
        try {
            return isBlank(value) ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean isAuthenticated(HttpSession session) {
        return session != null && session.getAttribute("userId") != null;
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equalsIgnoreCase((String) session.getAttribute("role"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
