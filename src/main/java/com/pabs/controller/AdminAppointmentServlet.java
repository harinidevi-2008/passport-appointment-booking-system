package com.pabs.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pabs.dao.AppointmentDAO;
import com.pabs.dao.AppointmentSlotDAO;
import com.pabs.dao.EmailNotificationDAO;
import com.pabs.dao.PassportApplicationDAO;
import com.pabs.dao.PassportOfficeDAO;
import com.pabs.model.Appointment;
import com.pabs.model.AppointmentSlot;
import com.pabs.model.PassportApplication;
import com.pabs.model.PassportOffice;
import com.pabs.service.AppointmentNotificationService;
import com.pabs.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/admin-appointments")
public class AdminAppointmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AdminAppointmentServlet.class.getName());

    private static final String FILTER_UPCOMING = "UPCOMING";
    private static final String FILTER_ALL = "ALL";

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final PassportApplicationDAO applicationDAO = new PassportApplicationDAO();
    private final AppointmentSlotDAO slotDAO = new AppointmentSlotDAO();
    private final PassportOfficeDAO officeDAO = new PassportOfficeDAO();
    private final EmailNotificationDAO emailNotificationDAO = new EmailNotificationDAO();
    private final EmailService emailService = new EmailService();
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

        String action = clean(request.getParameter("action"));
        if ("details".equals(action)) {
            showAppointmentDetails(request, response);
            return;
        }

        showAppointmentQueue(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
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

        Integer appointmentId = parseInt(request.getParameter("appointmentId"));
        String action = clean(request.getParameter("action"));
        if (appointmentId == null) {
            response.sendRedirect("admin-appointments?status=ALL&statusError=1");
            return;
        }

        boolean updated;
        String newStatus;
        if ("markAttended".equals(action)) {
            updated = appointmentDAO.markAttended(appointmentId);
            newStatus = "ATTENDED";
        } else if ("markCompleted".equals(action)) {
            updated = appointmentDAO.markCompleted(appointmentId);
            newStatus = "COMPLETED";
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String redirectUrl = "admin-appointments?action=details&id=" + appointmentId;
        if (updated) {
            boolean emailFailed = !sendStatusEmail(appointmentId, newStatus);
            redirectUrl += "&statusUpdated=1";
            if (emailFailed) {
                redirectUrl += "&mailError=1";
            }
        } else {
            redirectUrl += "&statusError=1";
        }
        response.sendRedirect(redirectUrl);
    }

    private boolean sendStatusEmail(int appointmentId, String status) {
        AdminAppointmentView view = buildView(appointmentDAO.findById(appointmentId));
        if (view == null) {
            LOGGER.warning("Admin appointment status email skipped because appointment details could not be loaded. appointmentId="
                    + appointmentId);
            return false;
        }

        String notificationType = "APPOINTMENT_" + status;
        String subject = "ATTENDED".equals(status)
                ? EmailService.SUBJECT_APPOINTMENT_ATTENDED
                : EmailService.SUBJECT_APPOINTMENT_COMPLETED;
        String to = view.getApplication().getEmail();
        try {
            if ("ATTENDED".equals(status)) {
                emailService.sendAppointmentAttendedEmail(
                        to,
                        view.getApplication().getFullName(),
                        view.getAppointment().getAppointmentNumber(),
                        view.getApplication().getApplicationNumber(),
                        view.getOffice().getOfficeName(),
                        view.getSlot().getAppointmentDate(),
                        view.getSlot().getStartTime(),
                        view.getSlot().getEndTime()
                );
            } else if ("COMPLETED".equals(status)) {
                emailService.sendAppointmentCompletedEmail(
                        to,
                        view.getApplication().getFullName(),
                        view.getAppointment().getAppointmentNumber(),
                        view.getApplication().getApplicationNumber(),
                        view.getOffice().getOfficeName(),
                        view.getSlot().getAppointmentDate(),
                        view.getSlot().getStartTime(),
                        view.getSlot().getEndTime()
                );
                emailService.sendApplicationStatusTrackingEmail(
                        to,
                        view.getApplication().getFullName(),
                        view.getApplication().getApplicationNumber(),
                        "PROCESSING",
                        "Your appointment has been completed and your application is now in post-appointment processing."
                );
                recordEmail(view, "APPLICATION_PROCESSING", to,
                        EmailService.SUBJECT_APPLICATION_PROCESSING, true, null);
            }

            recordEmail(view, notificationType, to, subject, true, null);
            LOGGER.info(() -> "Admin appointment status email sent. appointmentId="
                    + appointmentId + ", status=" + status);
            return true;
        } catch (MessagingException e) {
            recordEmail(view, notificationType, to, subject, false, e.getMessage());
            LOGGER.log(Level.WARNING,
                    "Admin appointment status email failed. appointmentId=" + appointmentId
                            + ", status=" + status
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            return false;
        }
    }

    private void recordEmail(AdminAppointmentView view,
                             String notificationType,
                             String recipientEmail,
                             String subject,
                             boolean sent,
                             String errorMessage) {
        emailNotificationDAO.record(
                view.getAppointment().getUserId(),
                view.getApplication().getId(),
                view.getAppointment().getId(),
                notificationType,
                recipientEmail,
                subject,
                sent,
                errorMessage
        );
    }

    private void showAppointmentQueue(HttpServletRequest request,
                                      HttpServletResponse response)
            throws ServletException, IOException {

        String statusFilter = clean(request.getParameter("status")).toUpperCase();
        if (statusFilter.isEmpty()) {
            statusFilter = FILTER_UPCOMING;
        }

        List<AdminAppointmentView> appointmentViews = new ArrayList<>();
        for (Appointment appointment : appointmentDAO.findAllAppointments()) {
            AdminAppointmentView appointmentView = buildView(appointment);
            if (appointmentView != null && matchesFilter(appointmentView, statusFilter)) {
                appointmentViews.add(appointmentView);
            }
        }

        request.setAttribute("appointmentViews", appointmentViews);
        request.setAttribute("selectedStatus", statusFilter);
        request.getRequestDispatcher("admin-appointments.jsp").forward(request, response);
    }

    private void showAppointmentDetails(HttpServletRequest request,
                                        HttpServletResponse response)
            throws ServletException, IOException {

        Integer appointmentId = parseInt(request.getParameter("id"));
        if (appointmentId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Appointment appointment = appointmentDAO.findById(appointmentId);
        AdminAppointmentView appointmentView = buildView(appointment);
        if (appointmentView == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("appointmentView", appointmentView);
        request.getRequestDispatcher("admin-appointment-details.jsp").forward(request, response);
    }

    private AdminAppointmentView buildView(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        PassportApplication application = applicationDAO.getApplicationById(appointment.getApplicationId());
        AppointmentSlot slot = slotDAO.findById(appointment.getSlotId());
        if (application == null || slot == null) {
            return null;
        }

        PassportOffice office = officeDAO.findById(slot.getOfficeId());
        if (office == null) {
            return null;
        }

        return new AdminAppointmentView(appointment, application, slot, office);
    }

    private boolean matchesFilter(AdminAppointmentView appointmentView, String statusFilter) {
        if (FILTER_ALL.equals(statusFilter)) {
            return true;
        }

        if (FILTER_UPCOMING.equals(statusFilter)) {
            return appointmentView.isUpcoming();
        }

        return statusFilter.equals(appointmentView.getAppointment().getStatus());
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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class AdminAppointmentView {
        private final Appointment appointment;
        private final PassportApplication application;
        private final AppointmentSlot slot;
        private final PassportOffice office;

        public AdminAppointmentView(Appointment appointment,
                                    PassportApplication application,
                                    AppointmentSlot slot,
                                    PassportOffice office) {
            this.appointment = appointment;
            this.application = application;
            this.slot = slot;
            this.office = office;
        }

        public Appointment getAppointment() {
            return appointment;
        }

        public PassportApplication getApplication() {
            return application;
        }

        public AppointmentSlot getSlot() {
            return slot;
        }

        public PassportOffice getOffice() {
            return office;
        }

        public boolean isUpcoming() {
            String status = appointment.getStatus();
            if (!("BOOKED".equals(status) || "RESCHEDULED".equals(status))) {
                return false;
            }

            return LocalDateTime.of(slot.getAppointmentDate(), slot.getEndTime())
                    .isAfter(LocalDateTime.now());
        }

        public boolean isAttendanceMarkAllowed() {
            String status = appointment.getStatus();
            if (!("BOOKED".equals(status) || "RESCHEDULED".equals(status))) {
                return false;
            }
            if (!"VERIFIED".equals(application.getStatus())) {
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startWindow = LocalDateTime.of(slot.getAppointmentDate(), slot.getStartTime())
                    .minusMinutes(15);
            LocalDateTime end = LocalDateTime.of(slot.getAppointmentDate(), slot.getEndTime());
            return !now.isBefore(startWindow) && now.isBefore(end);
        }

        public boolean isCompletionMarkAllowed() {
            return "ATTENDED".equals(appointment.getStatus())
                    && "VERIFIED".equals(application.getStatus());
        }
    }
}
