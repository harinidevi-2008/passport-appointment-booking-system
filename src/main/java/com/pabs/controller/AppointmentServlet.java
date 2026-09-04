package com.pabs.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.pabs.service.AppointmentRecommendationService;
import com.pabs.service.AppointmentRecommendationService.OfficeDayMetrics;
import com.pabs.service.AppointmentRecommendationService.OfficeRecommendation;
import com.pabs.service.AppointmentRecommendationService.SlotRecommendation;
import com.pabs.service.EmailService;
import com.pabs.util.CsrfUtil;

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
    private static final String APPOINTMENT_ELIGIBLE_APPLICATION_STATUS = "VERIFIED";

    private final PassportApplicationDAO applicationDAO = new PassportApplicationDAO();
    private final PassportOfficeDAO officeDAO = new PassportOfficeDAO();
    private final AppointmentSlotDAO slotDAO = new AppointmentSlotDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final EmailNotificationDAO emailNotificationDAO = new EmailNotificationDAO();
    private final EmailService emailService = new EmailService();
    private final AppointmentNotificationService appointmentNotificationService = new AppointmentNotificationService();
    private final AppointmentRecommendationService recommendationService = new AppointmentRecommendationService();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isLoggedInUser(session)) {
            redirectBySession(session, response);
            return;
        }

        appointmentNotificationService.expirePastAppointmentsAndNotifyNoShow();

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

        if (!CsrfUtil.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

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

        PassportApplication selectedApplication = selectedApplicationId == null
                ? null
                : getUserApplication(selectedApplicationId, userId);
        if (selectedApplication != null
                && isEligibleForAppointmentBooking(selectedApplication)
                && appointmentDAO.findActiveAppointmentForApplication(selectedApplicationId, userId) == null
                && !appointmentDAO.hasCompletedAppointmentForApplication(selectedApplicationId, userId)) {
            request.setAttribute("selectedApplicationId", selectedApplicationId);
            ensureNearFutureSlots(offices);
            request.setAttribute("officeRecommendations",
                    recommendationService.recommendOffices(selectedApplication, offices, LocalDate.now()));
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

        if (!isEligibleForAppointmentBooking(application)) {
            request.setAttribute("errorMessage", appointmentEligibilityMessage(application));
            showBookingForm(request, response, session);
            return;
        }

        if (appointmentDAO.findActiveAppointmentForApplication(applicationId, userId) != null) {
            request.setAttribute("errorMessage", "This application already has an active appointment.");
            showBookingForm(request, response, session);
            return;
        }

        if (appointmentDAO.hasCompletedAppointmentForApplication(applicationId, userId)) {
            request.setAttribute("errorMessage",
                    "This application already has a completed appointment. Normal appointment booking is no longer available.");
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
        OfficeDayMetrics officeDayMetrics = recommendationService.loadOfficeDayMetrics(officeId, appointmentDate);

        request.setAttribute("application", application);
        request.setAttribute("office", office);
        request.setAttribute("appointmentDate", appointmentDate);
        request.setAttribute("slotViews", slotViews);
        request.setAttribute("officeDayMetrics", officeDayMetrics);
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
        OfficeDayMetrics officeDayMetrics = recommendationService.loadOfficeDayMetrics(officeId, appointmentDate);

        request.setAttribute("application", appointmentView.getApplication());
        request.setAttribute("office", office);
        request.setAttribute("appointmentDate", appointmentDate);
        request.setAttribute("slotViews", slotViews);
        request.setAttribute("officeDayMetrics", officeDayMetrics);
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

        if (!isEligibleForAppointmentBooking(application)) {
            request.setAttribute("errorMessage", appointmentEligibilityMessage(application));
            showBookingForm(request, response, session);
            return;
        }

        if (appointmentDAO.findActiveAppointmentForApplication(applicationId, userId) != null) {
            request.setAttribute("errorMessage", "This application already has an active appointment.");
            showBookingForm(request, response, session);
            return;
        }

        if (appointmentDAO.hasCompletedAppointmentForApplication(applicationId, userId)) {
            request.setAttribute("errorMessage",
                    "This application already has a completed appointment. Normal appointment booking is no longer available.");
            showBookingForm(request, response, session);
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

        AppointmentView appointmentView = buildAppointmentView(appointment, userId);
        boolean emailFailed = !sendBookingEmail(session, appointmentView);
        String redirectUrl = "appointment?action=details&id=" + appointment.getId() + "&booked=1";
        if (emailFailed) {
            redirectUrl += "&mailError=1";
        }
        response.sendRedirect(redirectUrl);
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
        Appointment activeAppointment = appointmentDAO.findActiveAppointmentForApplication(
                appointmentView.getApplication().getId(), userId);
        boolean canBookSameApplication = "NO_SHOW".equals(appointmentView.getAppointment().getStatus())
                && isEligibleForAppointmentBooking(appointmentView.getApplication())
                && activeAppointment == null;
        request.setAttribute("canBookSameApplication", canBookSameApplication);
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
        Appointment appointment = appointmentDAO.findByIdForUser(appointmentId, userId);
        AppointmentView appointmentView = buildAppointmentView(appointment, userId);

        if (appointmentView == null || !appointmentView.isCancellable()) {
            response.sendRedirect("appointment?action=my&cancelError=1");
            return;
        }

        if (appointmentDAO.cancelAppointment(appointmentId, userId)) {
            boolean emailFailed = !sendCancellationEmail(session, appointmentView);
            String redirectUrl = "appointment?action=my&cancelled=1";
            if (emailFailed) {
                redirectUrl += "&mailError=1";
            }
            response.sendRedirect(redirectUrl);
        } else {
            response.sendRedirect("appointment?action=my&cancelError=1");
        }
    }

    private boolean sendBookingEmail(HttpSession session, AppointmentView appointmentView) {

        if (appointmentView == null) {
            LOGGER.warning("Appointment booking email skipped because appointment details could not be loaded.");
            return false;
        }

        String to = getRecipientEmail(session, appointmentView);
        String fullName = getRecipientName(session, appointmentView);

        try {
            emailService.sendAppointmentBookedEmail(
                    to,
                    fullName,
                    appointmentView.getAppointment().getAppointmentNumber(),
                    appointmentView.getApplication().getApplicationNumber(),
                    appointmentView.getOffice().getOfficeName(),
                    appointmentView.getSlot().getAppointmentDate(),
                    appointmentView.getSlot().getStartTime(),
                    appointmentView.getSlot().getEndTime(),
                    appointmentView.getAppointment().getStatus()
            );
            recordEmail(appointmentView, "APPOINTMENT_BOOKED", to,
                    EmailService.SUBJECT_APPOINTMENT_BOOKED, true, null);
            LOGGER.info(() -> "Appointment booking email sent. appointmentId="
                    + appointmentView.getAppointment().getId()
                    + ", appointmentNumber=" + appointmentView.getAppointment().getAppointmentNumber());
            return true;
        } catch (MessagingException e) {
            recordEmail(appointmentView, "APPOINTMENT_BOOKED", to,
                    EmailService.SUBJECT_APPOINTMENT_BOOKED, false, e.getMessage());
            LOGGER.log(Level.WARNING,
                    "Appointment booking email failed. appointmentId="
                            + appointmentView.getAppointment().getId()
                            + ", appointmentNumber=" + appointmentView.getAppointment().getAppointmentNumber()
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            return false;
        }
    }

    private boolean sendCancellationEmail(HttpSession session, AppointmentView appointmentView) {

        if (appointmentView == null) {
            LOGGER.warning("Appointment cancellation email skipped because appointment details could not be loaded.");
            return false;
        }

        String to = getRecipientEmail(session, appointmentView);
        String fullName = getRecipientName(session, appointmentView);

        try {
            emailService.sendAppointmentCancelledEmail(
                    to,
                    fullName,
                    appointmentView.getAppointment().getAppointmentNumber(),
                    appointmentView.getApplication().getApplicationNumber(),
                    appointmentView.getOffice().getOfficeName(),
                    appointmentView.getSlot().getAppointmentDate(),
                    appointmentView.getSlot().getStartTime(),
                    appointmentView.getSlot().getEndTime()
            );
            recordEmail(appointmentView, "APPOINTMENT_CANCELLED", to,
                    EmailService.SUBJECT_APPOINTMENT_CANCELLED, true, null);
            LOGGER.info(() -> "Appointment cancellation email sent. appointmentId="
                    + appointmentView.getAppointment().getId()
                    + ", appointmentNumber=" + appointmentView.getAppointment().getAppointmentNumber());
            return true;
        } catch (MessagingException e) {
            recordEmail(appointmentView, "APPOINTMENT_CANCELLED", to,
                    EmailService.SUBJECT_APPOINTMENT_CANCELLED, false, e.getMessage());
            LOGGER.log(Level.WARNING,
                    "Appointment cancellation email failed. appointmentId="
                            + appointmentView.getAppointment().getId()
                            + ", appointmentNumber=" + appointmentView.getAppointment().getAppointmentNumber()
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            return false;
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
            emailService.sendAppointmentRescheduledEmail(
                    to,
                    getRecipientName(session, newAppointmentView),
                    newAppointmentView.getAppointment().getAppointmentNumber(),
                    newAppointmentView.getApplication().getApplicationNumber(),
                    newAppointmentView.getOffice().getOfficeName(),
                    newAppointmentView.getSlot().getAppointmentDate(),
                    newAppointmentView.getSlot().getStartTime(),
                    newAppointmentView.getSlot().getEndTime(),
                    newAppointmentView.getAppointment().getStatus()
            );
            recordEmail(newAppointmentView, "APPOINTMENT_RESCHEDULED", to,
                    EmailService.SUBJECT_APPOINTMENT_RESCHEDULED, true, null);
            LOGGER.info(() -> "Appointment reschedule email sent. appointmentId="
                    + newAppointmentView.getAppointment().getId());
            return true;
        } catch (MessagingException e) {
            recordEmail(newAppointmentView, "APPOINTMENT_RESCHEDULED", to,
                    EmailService.SUBJECT_APPOINTMENT_RESCHEDULED, false, e.getMessage());
            LOGGER.log(Level.WARNING,
                    "Appointment reschedule email failed. appointmentId="
                            + newAppointmentView.getAppointment().getId()
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            return false;
        }
    }

    private String getRecipientEmail(HttpSession session, AppointmentView appointmentView) {
        String to = clean((String) session.getAttribute("email"));
        if (to.isEmpty() && appointmentView != null && appointmentView.getApplication() != null) {
            to = clean(appointmentView.getApplication().getEmail());
        }
        return to;
    }

    private String getRecipientName(HttpSession session, AppointmentView appointmentView) {
        String fullName = clean((String) session.getAttribute("fullName"));
        if (fullName.isEmpty() && appointmentView != null && appointmentView.getApplication() != null) {
            fullName = clean(appointmentView.getApplication().getFullName());
        }
        return fullName.isEmpty() ? "User" : fullName;
    }

    private void recordEmail(AppointmentView appointmentView,
                             String notificationType,
                             String recipientEmail,
                             String subject,
                             boolean sent,
                             String errorMessage) {
        if (appointmentView == null || appointmentView.getAppointment() == null
                || appointmentView.getApplication() == null) {
            return;
        }

        emailNotificationDAO.record(
                appointmentView.getAppointment().getUserId(),
                appointmentView.getApplication().getId(),
                appointmentView.getAppointment().getId(),
                notificationType,
                recipientEmail,
                subject,
                sent,
                errorMessage
        );
    }

    private String formatSchedule(AppointmentSlot slot) {
        return slot.getAppointmentDate() + " " + slot.getStartTime() + " - " + slot.getEndTime();
    }

    private List<PassportApplication> filterApplicationsWithoutActiveAppointment(
            List<PassportApplication> applications, int userId) {

        List<PassportApplication> eligibleApplications = new ArrayList<>();
        for (PassportApplication application : applications) {
            if (isEligibleForAppointmentBooking(application)
                    && appointmentDAO.findActiveAppointmentForApplication(application.getId(), userId) == null
                    && !appointmentDAO.hasCompletedAppointmentForApplication(application.getId(), userId)) {
                eligibleApplications.add(application);
            }
        }
        return eligibleApplications;
    }

    public static boolean isEligibleForAppointmentBooking(PassportApplication application) {
        return application != null
                && APPOINTMENT_ELIGIBLE_APPLICATION_STATUS.equals(application.getStatus());
    }

    public static String appointmentEligibilityMessage(PassportApplication application) {
        if (application == null) {
            return "Please select a valid passport application.";
        }

        String status = application.getStatus();
        if ("REJECTED".equals(status)) {
            return "Appointment booking is unavailable because this application was rejected.";
        }
        if ("SUBMITTED".equals(status) || "UNDER_REVIEW".equals(status)) {
            return "Appointment booking is not available yet. Your application must be verified before you can book an appointment.";
        }
        if ("APPROVED".equals(status)) {
            return "This application has already reached a final approved decision. Initial appointment booking is available only while the application is VERIFIED.";
        }
        if ("PROCESSING".equals(status)) {
            return "Your appointment is complete and this application is in processing. Normal appointment booking is no longer available.";
        }
        if ("PRINTING".equals(status)) {
            return "Your passport is printing. Normal appointment booking is no longer available.";
        }
        if ("DISPATCHED".equals(status)) {
            return "Your passport has been dispatched. Normal appointment booking is no longer available.";
        }
        if ("DELIVERED".equals(status)) {
            return "Your passport has been delivered. Normal appointment booking is no longer available.";
        }
        if ("CANCELLED".equals(status)) {
            return "Appointment booking is unavailable because this application was cancelled.";
        }
        return "Appointment booking is available only for VERIFIED applications.";
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
        Map<Integer, Integer> bookingCounts = slotDAO.countActiveBookingsForSlots(slots);
        List<SlotRecommendation> recommendations = recommendationService.rankSlots(
                slots, slotId -> bookingCounts.getOrDefault(slotId, 0));
        for (SlotRecommendation recommendation : recommendations) {
            AppointmentSlot slot = recommendation.getSlot();
            if (!isFutureSlot(slot)) {
                continue;
            }

            slotViews.add(new AppointmentSlotView(slot, recommendation));
        }
        return slotViews;
    }

    private void ensureNearFutureSlots(List<PassportOffice> offices) {
        if (offices == null) {
            return;
        }

        LocalDate date = LocalDate.now();
        LocalDate endDate = date.plusDays(7);
        for (PassportOffice office : offices) {
            LocalDate cursor = date;
            while (!cursor.isAfter(endDate)) {
                slotDAO.ensureSlotsForDate(office.getId(), cursor);
                cursor = cursor.plusDays(1);
            }
        }
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
        private final int bookedCount;
        private final String crowdLevel;
        private final String reason;
        private final boolean recommended;

        public AppointmentSlotView(AppointmentSlot slot, SlotRecommendation recommendation) {
            this.slot = slot;
            this.remainingCapacity = recommendation.getRemainingCapacity();
            this.bookedCount = recommendation.getBookedCount();
            this.crowdLevel = recommendation.getCrowdLevel();
            this.reason = recommendation.getReason();
            this.recommended = recommendation.isRecommended();
        }

        public AppointmentSlot getSlot() {
            return slot;
        }

        public int getRemainingCapacity() {
            return remainingCapacity;
        }

        public int getBookedCount() {
            return bookedCount;
        }

        public String getCrowdLevel() {
            return crowdLevel;
        }

        public String getReason() {
            return reason;
        }

        public boolean isRecommended() {
            return recommended;
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
            if (!("BOOKED".equals(status) || "RESCHEDULED".equals(status))) {
                return false;
            }

            return LocalDateTime.of(slot.getAppointmentDate(), slot.getStartTime())
                    .isAfter(LocalDateTime.now());
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
