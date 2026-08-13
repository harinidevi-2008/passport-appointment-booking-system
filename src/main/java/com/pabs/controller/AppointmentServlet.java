package com.pabs.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pabs.dao.AppointmentDAO;
import com.pabs.dao.AppointmentSlotDAO;
import com.pabs.dao.PassportApplicationDAO;
import com.pabs.dao.PassportOfficeDAO;
import com.pabs.model.Appointment;
import com.pabs.model.AppointmentSlot;
import com.pabs.model.PassportApplication;
import com.pabs.model.PassportOffice;
import com.pabs.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/appointment")
public class AppointmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AppointmentServlet.class.getName());

    private final PassportApplicationDAO applicationDAO = new PassportApplicationDAO();
    private final PassportOfficeDAO officeDAO = new PassportOfficeDAO();
    private final AppointmentSlotDAO slotDAO = new AppointmentSlotDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final EmailService emailService = new EmailService();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isLoggedInUser(session)) {
            redirectBySession(session, response);
            return;
        }

        String action = clean(request.getParameter("action"));

        if (action.isEmpty() || "my".equals(action)) {
            showMyAppointments(request, response, session);
            return;
        }

        if ("book".equals(action)) {
            showBookingForm(request, response, session);
            return;
        }

        if ("slots".equals(action)) {
            showAvailableSlots(request, response, session);
            return;
        }

        if ("reschedule".equals(action)) {
            showRescheduleForm(request, response, session);
            return;
        }

        if ("rescheduleSlots".equals(action)) {
            showRescheduleSlots(request, response, session);
            return;
        }

        if ("details".equals(action)) {
            showAppointmentDetails(request, response, session);
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isLoggedInUser(session)) {
            redirectBySession(session, response);
            return;
        }

        String action = clean(request.getParameter("action"));

        if ("confirm".equals(action)) {
            confirmAppointment(request, response, session);
            return;
        }

        if ("cancel".equals(action)) {
            cancelAppointment(request, response, session);
            return;
        }

        if ("rescheduleConfirm".equals(action)) {
            confirmReschedule(request, response, session);
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void showBookingForm(HttpServletRequest request,
                                 HttpServletResponse response,
                                 HttpSession session)
            throws ServletException, IOException {

        int userId = (Integer) session.getAttribute("userId");
        List<PassportApplication> applications = applicationDAO.getApplicationsByUserId(userId);
        List<PassportApplication> eligibleApplications = filterApplicationsWithoutActiveAppointment(applications, userId);
        List<PassportOffice> offices = officeDAO.findActiveOffices();
        Integer selectedApplicationId = parseInt(request.getParameter("applicationId"));

        if (selectedApplicationId != null && getUserApplication(selectedApplicationId, userId) != null
                && appointmentDAO.findActiveAppointmentForApplication(selectedApplicationId, userId) == null) {
            request.setAttribute("selectedApplicationId", selectedApplicationId);
        }

        request.setAttribute("applications", applications);
        request.setAttribute("eligibleApplications", eligibleApplications);
        request.setAttribute("offices", offices);
        request.getRequestDispatcher("book-appointment.jsp").forward(request, response);
    }

    private void showAvailableSlots(HttpServletRequest request,
                                    HttpServletResponse response,
                                    HttpSession session)
            throws ServletException, IOException {

        Integer applicationId = parseInt(request.getParameter("applicationId"));
        Integer officeId = parseInt(request.getParameter("officeId"));
        LocalDate appointmentDate = parseDate(request.getParameter("date"));

        if (applicationId == null || officeId == null || appointmentDate == null) {
            request.setAttribute("errorMessage", "Please select a valid application, office, and appointment date.");
            showBookingForm(request, response, session);
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        PassportApplication application = getUserApplication(applicationId, userId);
        PassportOffice office = officeDAO.findById(officeId);

        if (application == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (appointmentDAO.findActiveAppointmentForApplication(applicationId, userId) != null) {
            request.setAttribute("errorMessage", "This application already has an active appointment.");
            showBookingForm(request, response, session);
            return;
        }

        if (office == null || !office.isActive()) {
            request.setAttribute("errorMessage", "Please select an active passport office.");
            showBookingForm(request, response, session);
            return;
        }

        if (appointmentDate.isBefore(LocalDate.now())) {
            request.setAttribute("errorMessage", "Please select an appointment date from today onward.");
            showBookingForm(request, response, session);
            return;
        }

        slotDAO.ensureSlotsForDate(officeId, appointmentDate);

        List<AppointmentSlotView> slotViews = buildSlotViews(
                slotDAO.findAvailableActiveSlots(officeId, appointmentDate)
        );

        request.setAttribute("application", application);
        request.setAttribute("office", office);
        request.setAttribute("appointmentDate", appointmentDate);
        request.setAttribute("slotViews", slotViews);
        request.getRequestDispatcher("available-slots.jsp").forward(request, response);
    }

    private void showRescheduleForm(HttpServletRequest request,
                                    HttpServletResponse response,
                                    HttpSession session)
            throws ServletException, IOException {

        Integer appointmentId = parseInt(request.getParameter("appointmentId"));
        if (appointmentId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        Appointment appointment = appointmentDAO.findByIdForUser(appointmentId, userId);
        AppointmentView appointmentView = buildAppointmentView(appointment, userId);

        if (appointmentView == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!appointmentView.isReschedulable()) {
            response.sendRedirect("appointment?action=my&rescheduleError=1");
            return;
        }

        request.setAttribute("appointmentView", appointmentView);
        request.setAttribute("offices", officeDAO.findActiveOffices());
        request.getRequestDispatcher("reschedule-appointment.jsp").forward(request, response);
    }

    private void showRescheduleSlots(HttpServletRequest request,
                                     HttpServletResponse response,
                                     HttpSession session)
            throws ServletException, IOException {

        Integer appointmentId = parseInt(request.getParameter("appointmentId"));
        Integer officeId = parseInt(request.getParameter("officeId"));
        LocalDate appointmentDate = parseDate(request.getParameter("date"));

        if (appointmentId == null || officeId == null || appointmentDate == null) {
            request.setAttribute("errorMessage", "Please select a valid office and appointment date.");
            showRescheduleForm(request, response, session);
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        Appointment appointment = appointmentDAO.findByIdForUser(appointmentId, userId);
        AppointmentView appointmentView = buildAppointmentView(appointment, userId);
        PassportOffice office = officeDAO.findById(officeId);

        if (appointmentView == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!appointmentView.isReschedulable()) {
            request.setAttribute("errorMessage", "This appointment is not eligible for rescheduling.");
            showRescheduleForm(request, response, session);
            return;
        }

        if (office == null || !office.isActive()) {
            request.setAttribute("errorMessage", "Please select an active passport office.");
            showRescheduleForm(request, response, session);
            return;
        }

        if (appointmentDate.isBefore(LocalDate.now())) {
            request.setAttribute("errorMessage", "Please select an appointment date from today onward.");
            showRescheduleForm(request, response, session);
            return;
        }

        slotDAO.ensureSlotsForDate(officeId, appointmentDate);

        List<AppointmentSlotView> slotViews = buildSlotViews(
                slotDAO.findAvailableActiveSlots(officeId, appointmentDate)
        );

        request.setAttribute("application", appointmentView.getApplication());
        request.setAttribute("office", office);
        request.setAttribute("appointmentDate", appointmentDate);
        request.setAttribute("slotViews", slotViews);
        request.setAttribute("isReschedule", Boolean.TRUE);
        request.setAttribute("appointmentView", appointmentView);
        request.getRequestDispatcher("available-slots.jsp").forward(request, response);
    }

    private void confirmAppointment(HttpServletRequest request,
                                    HttpServletResponse response,
                                    HttpSession session)
            throws ServletException, IOException {

        Integer applicationId = parseInt(request.getParameter("applicationId"));
        Integer slotId = parseInt(request.getParameter("slotId"));

        if (applicationId == null || slotId == null) {
            request.setAttribute("errorMessage", "Please select a valid appointment slot.");
            showBookingForm(request, response, session);
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        PassportApplication application = getUserApplication(applicationId, userId);
        AppointmentSlot slot = slotDAO.findById(slotId);

        if (application == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (slot == null || !slot.isActive()) {
            request.setAttribute("errorMessage", "The selected appointment slot is no longer available.");
            showBookingForm(request, response, session);
            return;
        }

        if (!isFutureSlot(slot)) {
            request.setAttribute("errorMessage", "Please select a future appointment date and time.");
            showBookingForm(request, response, session);
            return;
        }

        Appointment appointment = appointmentDAO.createAppointment(applicationId, userId, slotId);
        if (appointment == null) {
            request.setAttribute("errorMessage",
                    "Unable to book this appointment. The slot may be full or this application may already have an active appointment.");
            showBookingForm(request, response, session);
            return;
        }

        response.sendRedirect("appointment?action=details&id=" + appointment.getId() + "&booked=1");
    }

    private void confirmReschedule(HttpServletRequest request,
                                   HttpServletResponse response,
                                   HttpSession session)
            throws ServletException, IOException {

        Integer appointmentId = parseInt(request.getParameter("appointmentId"));
        Integer slotId = parseInt(request.getParameter("slotId"));

        if (appointmentId == null || slotId == null) {
            response.sendRedirect("appointment?action=my&rescheduleError=1");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        Appointment appointment = appointmentDAO.findByIdForUser(appointmentId, userId);
        AppointmentView oldAppointmentView = buildAppointmentView(appointment, userId);
        AppointmentSlot newSlot = slotDAO.findById(slotId);

        if (oldAppointmentView == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!oldAppointmentView.isReschedulable()
                || newSlot == null
                || !newSlot.isActive()
                || !isFutureSlot(newSlot)) {
            request.setAttribute("errorMessage", "Please select a future available appointment slot.");
            showRescheduleForm(request, response, session);
            return;
        }

        Appointment rescheduledAppointment = appointmentDAO.rescheduleAppointment(appointmentId, userId, slotId);
        if (rescheduledAppointment == null) {
            request.setAttribute("errorMessage",
                    "Unable to reschedule this appointment. The slot may no longer be available.");
            showRescheduleForm(request, response, session);
            return;
        }

        AppointmentView newAppointmentView = buildAppointmentView(rescheduledAppointment, userId);
        boolean emailFailed = !sendRescheduleEmail(session, oldAppointmentView, newAppointmentView);
        String redirectUrl = "appointment?action=details&id=" + rescheduledAppointment.getId() + "&rescheduled=1";
        if (emailFailed) {
            redirectUrl += "&mailError=1";
        }
        response.sendRedirect(redirectUrl);
    }

    private void showMyAppointments(HttpServletRequest request,
                                    HttpServletResponse response,
                                    HttpSession session)
            throws ServletException, IOException {

        int userId = (Integer) session.getAttribute("userId");
        List<Appointment> appointments = appointmentDAO.findAppointmentsByUserId(userId);
        request.setAttribute("appointmentViews", buildAppointmentViews(appointments, userId));
        request.getRequestDispatcher("my-appointments.jsp").forward(request, response);
    }

    private void showAppointmentDetails(HttpServletRequest request,
                                        HttpServletResponse response,
                                        HttpSession session)
            throws ServletException, IOException {

        Integer appointmentId = parseInt(request.getParameter("id"));
        if (appointmentId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        Appointment appointment = appointmentDAO.findByIdForUser(appointmentId, userId);
        if (appointment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        AppointmentView appointmentView = buildAppointmentView(appointment, userId);
        if (appointmentView == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("appointmentView", appointmentView);
        if ("1".equals(request.getParameter("booked"))) {
            request.getRequestDispatcher("appointment-confirmation.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("appointment-details.jsp").forward(request, response);
        }
    }

    private void cancelAppointment(HttpServletRequest request,
                                   HttpServletResponse response,
                                   HttpSession session)
            throws IOException {

        Integer appointmentId = parseInt(request.getParameter("appointmentId"));
        if (appointmentId == null) {
            response.sendRedirect("appointment?action=my&cancelError=1");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        if (appointmentDAO.cancelAppointment(appointmentId, userId)) {
            response.sendRedirect("appointment?action=my&cancelled=1");
        } else {
            response.sendRedirect("appointment?action=my&cancelError=1");
        }
    }

    private boolean sendRescheduleEmail(HttpSession session,
                                        AppointmentView oldAppointmentView,
                                        AppointmentView newAppointmentView) {

        if (oldAppointmentView == null || newAppointmentView == null) {
            return false;
        }

        String to = clean((String) session.getAttribute("email"));
        if (to.isEmpty()) {
            to = clean(newAppointmentView.getApplication().getEmail());
        }

        try {
            emailService.sendAppointmentRescheduled(
                    to,
                    newAppointmentView.getAppointment().getAppointmentNumber(),
                    newAppointmentView.getApplication().getApplicationNumber(),
                    newAppointmentView.getOffice().getOfficeName(),
                    formatSchedule(oldAppointmentView.getSlot()),
                    formatSchedule(newAppointmentView.getSlot())
            );
            LOGGER.info(() -> "Appointment reschedule email sent. appointmentId="
                    + newAppointmentView.getAppointment().getId());
            return true;
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING,
                    "Appointment reschedule email failed. appointmentId="
                            + newAppointmentView.getAppointment().getId()
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            return false;
        }
    }

    private String formatSchedule(AppointmentSlot slot) {
        return slot.getAppointmentDate() + " " + slot.getStartTime() + " - " + slot.getEndTime();
    }

    private List<PassportApplication> filterApplicationsWithoutActiveAppointment(
            List<PassportApplication> applications, int userId) {

        List<PassportApplication> eligibleApplications = new ArrayList<>();
        for (PassportApplication application : applications) {
            if (appointmentDAO.findActiveAppointmentForApplication(application.getId(), userId) == null) {
                eligibleApplications.add(application);
            }
        }
        return eligibleApplications;
    }

    private PassportApplication getUserApplication(int applicationId, int userId) {
        List<PassportApplication> applications = applicationDAO.getApplicationsByUserId(userId);
        for (PassportApplication application : applications) {
            if (application.getId() == applicationId) {
                return application;
            }
        }
        return null;
    }

    private List<AppointmentSlotView> buildSlotViews(List<AppointmentSlot> slots) {
        List<AppointmentSlotView> slotViews = new ArrayList<>();
        for (AppointmentSlot slot : slots) {
            if (!isFutureSlot(slot)) {
                continue;
            }

            int activeBookings = slotDAO.countActiveBookingsForSlot(slot.getId());
            int remainingCapacity = slot.getCapacity() - activeBookings;
            slotViews.add(new AppointmentSlotView(slot, Math.max(remainingCapacity, 0)));
        }
        return slotViews;
    }

    private boolean isFutureSlot(AppointmentSlot slot) {
        if (slot == null || slot.getAppointmentDate() == null || slot.getStartTime() == null) {
            return false;
        }

        return LocalDateTime.of(slot.getAppointmentDate(), slot.getStartTime())
                .isAfter(LocalDateTime.now());
    }

    private List<AppointmentView> buildAppointmentViews(List<Appointment> appointments, int userId) {
        List<AppointmentView> appointmentViews = new ArrayList<>();
        Set<Integer> seenAppointments = new HashSet<>();

        for (Appointment appointment : appointments) {
            if (seenAppointments.add(appointment.getId())) {
                AppointmentView appointmentView = buildAppointmentView(appointment, userId);
                if (appointmentView != null) {
                    appointmentViews.add(appointmentView);
                }
            }
        }

        return appointmentViews;
    }

    private AppointmentView buildAppointmentView(Appointment appointment, int userId) {
        if (appointment == null) {
            return null;
        }

        PassportApplication application = getUserApplication(appointment.getApplicationId(), userId);
        AppointmentSlot slot = slotDAO.findById(appointment.getSlotId());

        if (application == null || slot == null) {
            return null;
        }

        PassportOffice office = officeDAO.findById(slot.getOfficeId());
        if (office == null) {
            return null;
        }

        return new AppointmentView(appointment, application, slot, office);
    }

    private boolean isLoggedInUser(HttpSession session) {
        return session != null
                && session.getAttribute("userId") != null
                && "USER".equalsIgnoreCase((String) session.getAttribute("role"));
    }

    private void redirectBySession(HttpSession session, HttpServletResponse response) throws IOException {
        if (session != null && session.getAttribute("userId") != null
                && "ADMIN".equalsIgnoreCase((String) session.getAttribute("role"))) {
            response.sendRedirect("admin-dashboard.jsp");
        } else {
            response.sendRedirect("login.jsp");
        }
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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class AppointmentSlotView {
        private final AppointmentSlot slot;
        private final int remainingCapacity;

        public AppointmentSlotView(AppointmentSlot slot, int remainingCapacity) {
            this.slot = slot;
            this.remainingCapacity = remainingCapacity;
        }

        public AppointmentSlot getSlot() {
            return slot;
        }

        public int getRemainingCapacity() {
            return remainingCapacity;
        }
    }

    public static class AppointmentView {
        private final Appointment appointment;
        private final PassportApplication application;
        private final AppointmentSlot slot;
        private final PassportOffice office;

        public AppointmentView(Appointment appointment, PassportApplication application,
                               AppointmentSlot slot, PassportOffice office) {
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

        public boolean isCancellable() {
            String status = appointment.getStatus();
            return "BOOKED".equals(status) || "RESCHEDULED".equals(status);
        }

        public boolean isReschedulable() {
            String status = appointment.getStatus();
            if (!("BOOKED".equals(status) || "RESCHEDULED".equals(status))) {
                return false;
            }

            return LocalDateTime.of(slot.getAppointmentDate(), slot.getStartTime())
                    .isAfter(LocalDateTime.now());
        }
    }
}
