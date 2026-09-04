package com.pabs.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pabs.dao.AppointmentDAO;
import com.pabs.dao.AppointmentSlotDAO;
import com.pabs.dao.DocumentDAO;
import com.pabs.dao.EmailNotificationDAO;
import com.pabs.dao.PassportApplicationDAO;
import com.pabs.dao.PassportOfficeDAO;
import com.pabs.model.Appointment;
import com.pabs.model.AppointmentSlot;
import com.pabs.model.Document;
import com.pabs.model.PassportApplication;
import com.pabs.model.PassportOffice;
import com.pabs.service.AppointmentNotificationService;
import com.pabs.service.EmailService;
import com.pabs.util.CsrfUtil;

import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/admin-applications")
public class AdminApplicationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AdminApplicationServlet.class.getName());

    private static final String SUBMITTED = "SUBMITTED";
    private static final String UNDER_REVIEW = "UNDER_REVIEW";
    private static final String VERIFIED = "VERIFIED";
    private static final String PROCESSING = "PROCESSING";
    private static final String APPROVED = "APPROVED";
    private static final String PRINTING = "PRINTING";
    private static final String DISPATCHED = "DISPATCHED";
    private static final String DELIVERED = "DELIVERED";
    private static final String REJECTED = "REJECTED";
    private static final String CANCELLED = "CANCELLED";

    private static final Set<String> ALLOWED_STATUSES =
            Set.of(SUBMITTED, UNDER_REVIEW, VERIFIED, PROCESSING, APPROVED,
                    PRINTING, DISPATCHED, DELIVERED, REJECTED, CANCELLED);

    private final PassportApplicationDAO applicationDAO = new PassportApplicationDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
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
            showApplicationDetails(request, response);
            return;
        }

        if ("document".equals(action)) {
            showDocument(request, response);
            return;
        }

        showApplicationQueue(request, response);
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

        String action = clean(request.getParameter("action"));
        if (!CsrfUtil.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!"updateStatus".equals(action)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        updateApplicationStatus(request, response, session);
    }

    private void showApplicationQueue(HttpServletRequest request,
                                      HttpServletResponse response)
            throws ServletException, IOException {

        List<AdminApplicationView> applicationViews = new ArrayList<>();
        String statusFilter = clean(request.getParameter("status")).toUpperCase();
        String search = clean(request.getParameter("search")).toLowerCase();

        for (PassportApplication application : applicationDAO.getAllApplications()) {
            if (!matchesStatusFilter(application, statusFilter) || !matchesSearch(application, search)) {
                continue;
            }
            applicationViews.add(buildView(application));
        }

        request.setAttribute("applicationViews", applicationViews);
        request.setAttribute("selectedStatus", statusFilter);
        request.setAttribute("search", search);
        request.getRequestDispatcher("admin-applications.jsp").forward(request, response);
    }

    private void showApplicationDetails(HttpServletRequest request,
                                        HttpServletResponse response)
            throws ServletException, IOException {

        Integer applicationId = parseInt(request.getParameter("id"));
        if (applicationId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        PassportApplication application = applicationDAO.getApplicationById(applicationId);
        if (application == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("applicationView", buildView(application));
        request.setAttribute("statusHistory", applicationDAO.getStatusHistoryByApplicationId(applicationId));
        request.setAttribute("nextStatuses", nextStatuses(application.getStatus()));
        request.getRequestDispatcher("admin-application-details.jsp").forward(request, response);
    }

    private void updateApplicationStatus(HttpServletRequest request,
                                         HttpServletResponse response,
                                         HttpSession session)
            throws IOException, ServletException {

        Integer applicationId = parseInt(request.getParameter("applicationId"));
        String requestedStatus = clean(request.getParameter("status")).toUpperCase();
        String reviewNote = clean(request.getParameter("reviewNote"));

        if (applicationId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        PassportApplication application = applicationDAO.getApplicationById(applicationId);
        if (application == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String validationError = validateStatusUpdate(application, requestedStatus, reviewNote);
        if (validationError != null) {
            request.setAttribute("errorMessage", validationError);
            request.setAttribute("applicationView", buildView(application));
            request.setAttribute("nextStatuses", nextStatuses(application.getStatus()));
            request.getRequestDispatcher("admin-application-details.jsp").forward(request, response);
            return;
        }

        String noteToStore = reviewNote.isEmpty() ? null : reviewNote;
        if (!REJECTED.equals(requestedStatus)) {
            noteToStore = null;
        }

        boolean updated = applicationDAO.updateApplicationStatus(
                applicationId, application.getStatus(), requestedStatus, noteToStore,
                (Integer) session.getAttribute("userId"));
        if (!updated) {
            LOGGER.warning("Application status update failed. applicationId=" + applicationId
                    + ", requestedStatus=" + requestedStatus);
            request.setAttribute("errorMessage", "Unable to update the application status. Please try again.");
            request.setAttribute("applicationView", buildView(application));
            request.setAttribute("nextStatuses", nextStatuses(application.getStatus()));
            request.getRequestDispatcher("admin-application-details.jsp").forward(request, response);
            return;
        }

        LOGGER.info(() -> "Application status updated. applicationId=" + applicationId
                + ", fromStatus=" + application.getStatus() + ", toStatus=" + requestedStatus);
        boolean emailSent = sendStatusEmail(application, requestedStatus, noteToStore);
        String redirectUrl = "admin-applications?action=details&id=" + applicationId + "&updated=1";
        if (!emailSent) {
            redirectUrl += "&mailError=1";
        }
        response.sendRedirect(redirectUrl);
    }

    private void showDocument(HttpServletRequest request,
                              HttpServletResponse response)
            throws IOException {

        Integer applicationId = parseInt(request.getParameter("id"));
        String documentType = clean(request.getParameter("type"));
        if (applicationId == null || !isAllowedDocumentType(documentType)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        PassportApplication application = applicationDAO.getApplicationById(applicationId);
        if (application == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Document document = documentDAO.findByUserId(application.getUserId());
        String filename = filenameForType(document, documentType);
        if (isBlank(filename)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path uploadDirectory = uploadDirectory();
        Path documentPath = uploadDirectory.resolve(filename).normalize();
        if (!documentPath.startsWith(uploadDirectory) || !Files.isRegularFile(documentPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(documentPath);
        if (isBlank(contentType)) {
            contentType = "application/octet-stream";
        }

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + safeDownloadName(documentType, filename) + "\"");
        response.setContentLengthLong(Files.size(documentPath));

        try (OutputStream output = response.getOutputStream()) {
            Files.copy(documentPath, output);
        }
    }

    private String validateStatusUpdate(PassportApplication application,
                                        String requestedStatus,
                                        String reviewNote) {

        if (!ALLOWED_STATUSES.contains(requestedStatus)) {
            return "Please select a valid application status.";
        }

        if (!nextStatuses(application.getStatus()).contains(requestedStatus)) {
            return "The requested status transition is not allowed.";
        }

        if (REJECTED.equals(requestedStatus) && isBlank(reviewNote)) {
            return "A rejection reason is required when rejecting an application.";
        }

        if (APPROVED.equals(requestedStatus)) {
            if (!PROCESSING.equals(application.getStatus())) {
                return "Application cannot be approved before the applicant's appointment has been completed.";
            }
            if (!applicationDAO.hasCompletedAppointment(application.getId())) {
                return "Application cannot be approved before the applicant's appointment has been completed.";
            }
            Document document = documentDAO.findByUserId(application.getUserId());
            if (!hasAllRequiredDocuments(document)) {
                return "All required documents must be uploaded before approving this application.";
            }
        }

        return null;
    }

    private boolean sendStatusEmail(PassportApplication application, String status, String reviewNote) {
        String notificationType = "APPLICATION_" + status;
        String subject = applicationSubject(status);
        try {
            if (UNDER_REVIEW.equals(status)) {
                emailService.sendApplicationUnderReviewEmail(
                        application.getEmail(),
                        application.getFullName(),
                        application.getApplicationNumber(),
                        status
                );
            } else if (VERIFIED.equals(status)) {
                emailService.sendApplicationVerifiedEmail(
                        application.getEmail(),
                        application.getFullName(),
                        application.getApplicationNumber(),
                        status
                );
            } else if (APPROVED.equals(status)) {
                emailService.sendApplicationApprovedEmail(
                        application.getEmail(),
                        application.getFullName(),
                        application.getApplicationNumber(),
                        status,
                        appointmentInfo(application)
                );
            } else if (PROCESSING.equals(status)
                    || PRINTING.equals(status)
                    || DISPATCHED.equals(status)
                    || DELIVERED.equals(status)) {
                emailService.sendApplicationStatusTrackingEmail(
                        application.getEmail(),
                        application.getFullName(),
                        application.getApplicationNumber(),
                        status,
                        statusMessage(status)
                );
            } else if (REJECTED.equals(status)) {
                emailService.sendApplicationRejectedEmail(
                        application.getEmail(),
                        application.getFullName(),
                        application.getApplicationNumber(),
                        status,
                        reviewNote
                );
            } else {
                return true;
            }
            recordEmail(application.getUserId(), application.getId(), null, notificationType,
                    application.getEmail(), subject, true, null);
            LOGGER.info(() -> "Application status email sent. applicationId=" + application.getId()
                    + ", applicationNumber=" + application.getApplicationNumber()
                    + ", status=" + status);
            return true;
        } catch (MessagingException e) {
            recordEmail(application.getUserId(), application.getId(), null, notificationType,
                    application.getEmail(), subject, false, e.getMessage());
            LOGGER.log(Level.WARNING,
                    "Application status email failed. applicationId=" + application.getId()
                            + ", applicationNumber=" + application.getApplicationNumber()
                            + ", status=" + status
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            return false;
        }
    }

    private String applicationSubject(String status) {
        if (UNDER_REVIEW.equals(status)) {
            return EmailService.SUBJECT_APPLICATION_UNDER_REVIEW;
        }
        if (VERIFIED.equals(status)) {
            return EmailService.SUBJECT_APPLICATION_VERIFIED;
        }
        if (APPROVED.equals(status)) {
            return EmailService.SUBJECT_APPLICATION_APPROVED;
        }
        if (PROCESSING.equals(status)) {
            return EmailService.SUBJECT_APPLICATION_PROCESSING;
        }
        if (PRINTING.equals(status)) {
            return EmailService.SUBJECT_PASSPORT_PRINTING;
        }
        if (DISPATCHED.equals(status)) {
            return EmailService.SUBJECT_PASSPORT_DISPATCHED;
        }
        if (DELIVERED.equals(status)) {
            return EmailService.SUBJECT_PASSPORT_DELIVERED;
        }
        if (REJECTED.equals(status)) {
            return EmailService.SUBJECT_APPLICATION_REJECTED;
        }
        return "Passport Application Status Updated";
    }

    private void recordEmail(Integer userId,
                             Integer applicationId,
                             Integer appointmentId,
                             String notificationType,
                             String recipientEmail,
                             String subject,
                             boolean sent,
                             String errorMessage) {
        emailNotificationDAO.record(userId, applicationId, appointmentId, notificationType,
                recipientEmail, subject, sent, errorMessage);
    }

    private String appointmentInfo(PassportApplication application) {
        AdminApplicationView view = buildView(application);
        if (view.getAppointment() == null || view.getSlot() == null || view.getOffice() == null) {
            return "You may book an appointment in PABS if appointment booking is available for this application.";
        }

        return "Appointment " + view.getAppointment().getAppointmentNumber()
                + " is scheduled at " + view.getOffice().getOfficeName()
                + " on " + view.getSlot().getAppointmentDate()
                + " from " + view.getSlot().getStartTime()
                + " to " + view.getSlot().getEndTime()
                + ".";
    }

    private Set<String> nextStatuses(String currentStatus) {
        Set<String> statuses = new LinkedHashSet<>();

        if (SUBMITTED.equals(currentStatus)) {
            statuses.add(UNDER_REVIEW);
        } else if (UNDER_REVIEW.equals(currentStatus)) {
            statuses.add(VERIFIED);
            statuses.add(REJECTED);
        } else if (VERIFIED.equals(currentStatus)) {
            statuses.add(REJECTED);
        } else if (PROCESSING.equals(currentStatus)) {
            statuses.add(APPROVED);
            statuses.add(REJECTED);
        } else if (APPROVED.equals(currentStatus)) {
            statuses.add(PRINTING);
        } else if (PRINTING.equals(currentStatus)) {
            statuses.add(DISPATCHED);
        } else if (DISPATCHED.equals(currentStatus)) {
            statuses.add(DELIVERED);
        }

        return statuses;
    }

    private String statusMessage(String status) {
        if (PROCESSING.equals(status)) {
            return "Your appointment has been completed and your application is now in post-appointment processing.";
        }
        if (PRINTING.equals(status)) {
            return "Your passport has moved to printing.";
        }
        if (DISPATCHED.equals(status)) {
            return "Your passport has been dispatched.";
        }
        if (DELIVERED.equals(status)) {
            return "Your passport has been delivered and the lifecycle is complete.";
        }
        return "Your passport application status has been updated.";
    }

    private AdminApplicationView buildView(PassportApplication application) {
        Document document = documentDAO.findByUserId(application.getUserId());
        Appointment appointment = appointmentDAO.findLatestByApplicationId(application.getId());
        AppointmentSlot slot = null;
        PassportOffice office = null;

        if (appointment != null) {
            try {
                slot = slotDAO.findById(appointment.getSlotId());
                if (slot != null) {
                    office = officeDAO.findById(slot.getOfficeId());
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING,
                        "Unable to load appointment schedule for admin application view. applicationId="
                                + application.getId() + ", exceptionType=" + e.getClass().getName()
                                + ", message=" + e.getMessage(),
                        e);
            }
        }

        return new AdminApplicationView(application, document, appointment, slot, office);
    }

    private boolean hasAllRequiredDocuments(Document document) {
        return document != null
                && !isBlank(document.getIdentityProof())
                && !isBlank(document.getAddressProof())
                && !isBlank(document.getPhotograph());
    }

    private boolean matchesStatusFilter(PassportApplication application, String statusFilter) {
        return isBlank(statusFilter) || "ALL".equals(statusFilter)
                || statusFilter.equals(application.getStatus());
    }

    private boolean matchesSearch(PassportApplication application, String search) {
        if (isBlank(search)) {
            return true;
        }

        return contains(application.getApplicationNumber(), search)
                || contains(application.getFullName(), search)
                || contains(application.getEmail(), search);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private boolean isAllowedDocumentType(String documentType) {
        return "identity".equals(documentType)
                || "address".equals(documentType)
                || "photograph".equals(documentType);
    }

    private String filenameForType(Document document, String documentType) {
        if (document == null) {
            return null;
        }

        if ("identity".equals(documentType)) {
            return document.getIdentityProof();
        }
        if ("address".equals(documentType)) {
            return document.getAddressProof();
        }
        if ("photograph".equals(documentType)) {
            return document.getPhotograph();
        }
        return null;
    }

    private Path uploadDirectory() {
        String configuredDirectory = clean(System.getenv("PABS_UPLOAD_DIR"));
        if (configuredDirectory.isEmpty()) {
            configuredDirectory = Paths.get(System.getProperty("user.home"), "pabs-uploads").toString();
        }

        return Paths.get(configuredDirectory).toAbsolutePath().normalize();
    }

    private String safeDownloadName(String documentType, String filename) {
        String extension = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            extension = filename.substring(dot);
        }
        return documentType + "-proof" + extension.replace("\"", "");
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

    public static class AdminApplicationView {
        private final PassportApplication application;
        private final Document document;
        private final Appointment appointment;
        private final AppointmentSlot slot;
        private final PassportOffice office;

        public AdminApplicationView(PassportApplication application,
                                    Document document,
                                    Appointment appointment,
                                    AppointmentSlot slot,
                                    PassportOffice office) {
            this.application = application;
            this.document = document;
            this.appointment = appointment;
            this.slot = slot;
            this.office = office;
        }

        public PassportApplication getApplication() {
            return application;
        }

        public Document getDocument() {
            return document;
        }

        public Appointment getAppointment() {
            return appointment;
        }

        public AppointmentSlot getSlot() {
            return slot;
        }

        public PassportOffice getOffice() {
            return office;
        }

        public boolean hasAllRequiredDocuments() {
            return document != null
                    && document.getIdentityProof() != null && !document.getIdentityProof().trim().isEmpty()
                    && document.getAddressProof() != null && !document.getAddressProof().trim().isEmpty()
                    && document.getPhotograph() != null && !document.getPhotograph().trim().isEmpty();
        }

        public String getDocumentStatus() {
            return hasAllRequiredDocuments() ? "Uploaded" : "Missing";
        }

        public String getAppointmentStatus() {
            return appointment == null ? "Not Booked" : appointment.getStatus();
        }

        public boolean hasIdentityProof() {
            return document != null
                    && document.getIdentityProof() != null
                    && !document.getIdentityProof().trim().isEmpty();
        }

        public boolean hasAddressProof() {
            return document != null
                    && document.getAddressProof() != null
                    && !document.getAddressProof().trim().isEmpty();
        }

        public boolean hasPhotograph() {
            return document != null
                    && document.getPhotograph() != null
                    && !document.getPhotograph().trim().isEmpty();
        }
    }
}
