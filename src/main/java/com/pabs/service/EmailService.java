package com.pabs.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailService {

    private static final String DEFAULT_HOST = "smtp.gmail.com";
    private static final String DEFAULT_PORT = "587";
    public static final String SUBJECT_APPLICATION_SUBMITTED = "Passport Application Submitted";
    public static final String SUBJECT_APPLICATION_UNDER_REVIEW = "Passport Application Under Review";
    public static final String SUBJECT_APPLICATION_VERIFIED = "Passport Application Verified";
    public static final String SUBJECT_APPLICATION_PROCESSING = "Passport Application Processing";
    public static final String SUBJECT_APPLICATION_APPROVED = "Passport Application Approved";
    public static final String SUBJECT_APPLICATION_REJECTED = "Passport Application Rejected";
    public static final String SUBJECT_APPLICATION_WITHDRAWN = "Passport Application Withdrawn";
    public static final String SUBJECT_PASSPORT_PRINTING = "Passport Printing";
    public static final String SUBJECT_PASSPORT_DISPATCHED = "Passport Dispatched";
    public static final String SUBJECT_PASSPORT_DELIVERED = "Passport Delivered";
    public static final String SUBJECT_APPOINTMENT_BOOKED = "Passport Appointment Confirmed";
    public static final String SUBJECT_APPOINTMENT_RESCHEDULED = "Passport Appointment Rescheduled";
    public static final String SUBJECT_APPOINTMENT_CANCELLED = "Passport Appointment Cancelled";
    public static final String SUBJECT_APPOINTMENT_ATTENDED = "Passport Appointment Visit Recorded";
    public static final String SUBJECT_APPOINTMENT_COMPLETED = "Passport Appointment Completed";
    public static final String SUBJECT_APPOINTMENT_NO_SHOW = "Passport Appointment Missed - Application Still Active";

    private final String username;
    private final String password;
    private final String fromAddress;
    private final Session session;

    public EmailService() {
        this.username = getEnv("PABS_SMTP_USERNAME", "PABS_MAIL_USERNAME");
        this.password = getEnv("PABS_SMTP_PASSWORD", "PABS_MAIL_PASSWORD");
        this.fromAddress = valueOrDefault(getEnv("PABS_SMTP_FROM", "PABS_MAIL_FROM"), username);

        String host = valueOrDefault(getEnv("PABS_SMTP_HOST", "PABS_MAIL_HOST"), DEFAULT_HOST);
        String port = valueOrDefault(getEnv("PABS_SMTP_PORT", "PABS_MAIL_PORT"), DEFAULT_PORT);

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);

        this.session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    public void sendEmail(String to, String subject, String htmlBody) throws MessagingException {
        if (isBlank(username) || isBlank(password) || isBlank(fromAddress)) {
            throw new MessagingException("SMTP configuration is incomplete.");
        }

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    public void sendPasswordResetOtp(String to, String otp) throws MessagingException {
        sendEmail(
                to,
                "Passport Appointment Booking System - Password Reset OTP",
                template(
                        "Password Reset OTP",
                        "Use this OTP to reset your Passport Appointment Booking System password.",
                        rows(row("OTP", otp), row("Valid For", "5 minutes")),
                        "If you did not request a password reset, you can ignore this email."
                )
        );
    }

    public void sendRegistrationSuccessEmail(String to, String fullName) throws MessagingException {
        sendEmail(
                to,
                "Passport Appointment Booking System - Registration Successful",
                template(
                        "Registration Successful",
                        "Dear " + esc(displayName(fullName)) + ", your account was created successfully.",
                        rows(row("Registered Email", to)),
                        "You can now log in and use the Passport Appointment Booking System."
                )
        );
    }

    public void sendApplicationSubmittedEmail(String to,
                                              String fullName,
                                              String applicationNumber,
                                              String applicationType,
                                              LocalDate submissionDate,
                                              String status) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPLICATION_SUBMITTED,
                template(
                        "Passport Application Submitted",
                        "Dear " + esc(displayName(fullName))
                                + ", your passport application has been submitted successfully.",
                        rows(
                                row("Citizen Name", fullName),
                                row("Application Number", applicationNumber),
                                row("Application Type", applicationType),
                                row("Submission Date", text(submissionDate)),
                                row("Current Status", status)
                        ),
                        "You can track this application from My Applications after logging in to PABS."
                )
        );
    }

    public void sendApplicationUnderReviewEmail(String to,
                                                String fullName,
                                                String applicationNumber,
                                                String status) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPLICATION_UNDER_REVIEW,
                template(
                        "Application Under Review",
                        "Dear " + esc(displayName(fullName))
                                + ", your passport application is now being reviewed by PABS staff.",
                        rows(
                                row("Citizen Name", fullName),
                                row("Application Number", applicationNumber),
                                row("Updated Status", status)
                        ),
                        "We will notify you when the review is complete or if further action is required."
                )
        );
    }

    public void sendApplicationVerifiedEmail(String to,
                                             String fullName,
                                             String applicationNumber,
                                             String status) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPLICATION_VERIFIED,
                template(
                        "Application Verified",
                        "Dear " + esc(displayName(fullName))
                                + ", your passport application has been verified.",
                        rows(
                                row("Citizen Name", fullName),
                                row("Application Number", applicationNumber),
                                row("Updated Status", status),
                                row("Next Step", "You may book an appointment through PABS when no active appointment exists for this application.")
                        ),
                        "Please log in to PABS to view available appointment slots."
                )
        );
    }

    public void sendApplicationApprovedEmail(String to,
                                             String fullName,
                                             String applicationNumber,
                                             String status,
                                             String appointmentInfo) throws MessagingException {
        String nextStep = isBlank(appointmentInfo)
                ? "Please log in to PABS to view any available next steps for this application."
                : appointmentInfo;
        sendEmail(
                to,
                SUBJECT_APPLICATION_APPROVED,
                template(
                        "Passport Application Approved",
                        "Dear " + esc(displayName(fullName)) + ", your passport application has been approved.",
                        rows(
                                row("Citizen Name", fullName),
                                row("Application Number", applicationNumber),
                                row("Updated Status", status),
                                row("Next Step", nextStep)
                        ),
                        "This is the final positive application decision. Please continue using PABS for any appointment or application records."
                )
        );
    }

    public void sendApplicationStatusTrackingEmail(String to,
                                                   String fullName,
                                                   String applicationNumber,
                                                   String status,
                                                   String statusMessage) throws MessagingException {
        sendEmail(
                to,
                subjectForStatus(status),
                template(
                        headingForStatus(status),
                        "Dear " + esc(displayName(fullName))
                                + ", your passport application status has been updated.",
                        rows(
                                row("Citizen Name", fullName),
                                row("Application Number", applicationNumber),
                                row("Updated Status", status)
                        ),
                        statusMessage
                )
        );
    }

    public void sendApplicationRejectedEmail(String to,
                                             String fullName,
                                             String applicationNumber,
                                             String status,
                                             String rejectionReason) throws MessagingException {
        String reasonRow = isBlank(rejectionReason) ? "" : row("Rejection Reason", rejectionReason);
        sendEmail(
                to,
                SUBJECT_APPLICATION_REJECTED,
                template(
                        "Passport Application Update",
                        "Dear " + esc(displayName(fullName))
                                + ", your passport application was rejected. Please review the details below.",
                        rows(
                                row("Citizen Name", fullName),
                                row("Application Number", applicationNumber),
                                row("Status", status),
                                reasonRow
                        ),
                        "You can view this application under My Applications after logging in to PABS."
                )
        );
    }

    public void sendApplicationWithdrawnEmail(String to,
                                              String fullName,
                                              String applicationNumber) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPLICATION_WITHDRAWN,
                template(
                        "Passport Application Withdrawn",
                        "Dear " + esc(displayName(fullName))
                                + ", your passport application has been withdrawn successfully.",
                        rows(
                                row("Citizen Name", fullName),
                                row("Application Number", applicationNumber),
                                row("Updated Status", "CANCELLED")
                        ),
                        "Any active appointment associated with this application has also been cancelled. "
                                + "Previous appointment history has been retained in the system."
                )
        );
    }

    public void sendAppointmentBookedEmail(String to,
                                           String fullName,
                                           String appointmentNumber,
                                           String applicationNumber,
                                           String officeName,
                                           LocalDate appointmentDate,
                                           LocalTime startTime,
                                           LocalTime endTime,
                                           String status) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPOINTMENT_BOOKED,
                appointmentTemplate(
                        "Passport Appointment Confirmed",
                        "Dear " + esc(displayName(fullName)) + ", your passport appointment has been confirmed.",
                        fullName, appointmentNumber, applicationNumber, officeName,
                        appointmentDate, startTime, endTime, status,
                        "Please arrive at the passport office before your scheduled start time and carry the required original documents."
                )
        );
    }

    public void sendAppointmentRescheduledEmail(String to,
                                                String fullName,
                                                String appointmentNumber,
                                                String applicationNumber,
                                                String officeName,
                                                LocalDate appointmentDate,
                                                LocalTime startTime,
                                                LocalTime endTime,
                                                String status) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPOINTMENT_RESCHEDULED,
                appointmentTemplate(
                        "Passport Appointment Rescheduled",
                        "Dear " + esc(displayName(fullName)) + ", your passport appointment has been rescheduled.",
                        fullName, appointmentNumber, applicationNumber, officeName,
                        appointmentDate, startTime, endTime, status,
                        "Please use the new office, date, and time shown above."
                )
        );
    }

    public void sendAppointmentCancelledEmail(String to,
                                              String fullName,
                                              String appointmentNumber,
                                              String applicationNumber,
                                              String officeName,
                                              LocalDate appointmentDate,
                                              LocalTime startTime,
                                              LocalTime endTime) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPOINTMENT_CANCELLED,
                appointmentTemplate(
                        "Passport Appointment Cancelled",
                        "Dear " + esc(displayName(fullName)) + ", your passport appointment has been cancelled.",
                        fullName, appointmentNumber, applicationNumber, officeName,
                        appointmentDate, startTime, endTime, "CANCELLED",
                        "Only this appointment was cancelled. Your passport application remains unchanged and you may rebook when eligible."
                )
        );
    }

    public void sendAppointmentAttendedEmail(String to,
                                             String fullName,
                                             String appointmentNumber,
                                             String applicationNumber,
                                             String officeName,
                                             LocalDate appointmentDate,
                                             LocalTime startTime,
                                             LocalTime endTime) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPOINTMENT_ATTENDED,
                appointmentTemplate(
                        "Passport Appointment Visit Recorded",
                        "Dear " + esc(displayName(fullName))
                                + ", your attendance at the scheduled passport appointment has been recorded.",
                        fullName, appointmentNumber, applicationNumber, officeName,
                        appointmentDate, startTime, endTime, "ATTENDED",
                        "Your visit has been recorded. PABS staff will continue with the next processing steps for your application."
                )
        );
    }

    public void sendAppointmentCompletedEmail(String to,
                                              String fullName,
                                              String appointmentNumber,
                                              String applicationNumber,
                                              String officeName,
                                              LocalDate appointmentDate,
                                              LocalTime startTime,
                                              LocalTime endTime) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPOINTMENT_COMPLETED,
                appointmentTemplate(
                        "Passport Appointment Completed",
                        "Dear " + esc(displayName(fullName))
                                + ", your passport appointment process has been completed.",
                        fullName, appointmentNumber, applicationNumber, officeName,
                        appointmentDate, startTime, endTime, "COMPLETED",
                        "This confirms that the appointment process has been completed in PABS."
                )
        );
    }

    public void sendAppointmentNoShowEmail(String to,
                                           String fullName,
                                           String appointmentNumber,
                                           String applicationNumber,
                                           String officeName,
                                           LocalDate appointmentDate,
                                           LocalTime startTime,
                                           LocalTime endTime) throws MessagingException {
        sendEmail(
                to,
                SUBJECT_APPOINTMENT_NO_SHOW,
                appointmentTemplate(
                        "Passport Appointment Missed",
                        "Dear " + esc(displayName(fullName))
                                + ", your appointment was marked as NO_SHOW because you did not attend within the scheduled appointment window.",
                        fullName, appointmentNumber, applicationNumber, officeName,
                        appointmentDate, startTime, endTime, "NO_SHOW",
                        "Your passport application itself has NOT been cancelled. It remains active under the existing VERIFIED application rules, and you may rebook through PABS when eligible."
                )
        );
    }

    private String appointmentTemplate(String heading,
                                       String intro,
                                       String fullName,
                                       String appointmentNumber,
                                       String applicationNumber,
                                       String officeName,
                                       LocalDate appointmentDate,
                                       LocalTime startTime,
                                       LocalTime endTime,
                                       String status,
                                       String footer) {
        return template(
                heading,
                intro,
                rows(
                        row("Citizen Name", fullName),
                        row("Appointment Number", appointmentNumber),
                        row("Application Number", applicationNumber),
                        row("Passport Office", officeName),
                        row("Appointment Date", text(appointmentDate)),
                        row("Start Time", text(startTime)),
                        row("End Time", text(endTime)),
                        row("Current Status", status)
                ),
                footer
        );
    }

    private String subjectForStatus(String status) {
        if ("PROCESSING".equals(status)) {
            return SUBJECT_APPLICATION_PROCESSING;
        }
        if ("PRINTING".equals(status)) {
            return SUBJECT_PASSPORT_PRINTING;
        }
        if ("DISPATCHED".equals(status)) {
            return SUBJECT_PASSPORT_DISPATCHED;
        }
        if ("DELIVERED".equals(status)) {
            return SUBJECT_PASSPORT_DELIVERED;
        }
        return "Passport Application Status Updated";
    }

    private String headingForStatus(String status) {
        if ("PROCESSING".equals(status)) {
            return "Application Processing";
        }
        if ("PRINTING".equals(status)) {
            return "Passport Printing";
        }
        if ("DISPATCHED".equals(status)) {
            return "Passport Dispatched";
        }
        if ("DELIVERED".equals(status)) {
            return "Passport Delivered";
        }
        return "Application Status Updated";
    }

    private String template(String heading, String intro, String rows, String footer) {
        return "<!doctype html><html><body style=\"margin:0;background:#eef3f8;"
                + "font-family:Arial,sans-serif;color:#172033;\">"
                + "<div style=\"max-width:640px;margin:0 auto;padding:28px 16px;\">"
                + "<div style=\"background:#0b2240;color:#fff;padding:20px 24px;border-radius:8px 8px 0 0;\">"
                + "<div style=\"font-size:13px;font-weight:700;color:#f8c44f;\">Passport Appointment Booking System</div>"
                + "<h1 style=\"margin:8px 0 0;font-size:24px;line-height:1.25;\">" + esc(heading) + "</h1>"
                + "</div>"
                + "<div style=\"background:#fff;border:1px solid #dce4ef;border-top:0;padding:24px;border-radius:0 0 8px 8px;\">"
                + "<p style=\"margin:0 0 18px;line-height:1.6;\">" + intro + "</p>"
                + "<table style=\"width:100%;border-collapse:collapse;margin:0 0 20px;\">"
                + rows
                + "</table>"
                + "<p style=\"margin:0;color:#5f6f86;line-height:1.6;\">" + esc(footer) + "</p>"
                + "</div></div></body></html>";
    }

    private String rows(String... rows) {
        StringBuilder builder = new StringBuilder();
        for (String row : rows) {
            if (!isBlank(row)) {
                builder.append(row);
            }
        }
        return builder.toString();
    }

    private String row(String label, Object value) {
        if (value == null || isBlank(String.valueOf(value))) {
            return "";
        }
        return "<tr>"
                + "<td style=\"width:42%;padding:10px 12px;border-top:1px solid #e7edf5;"
                + "font-weight:700;color:#344054;\">" + esc(label) + "</td>"
                + "<td style=\"padding:10px 12px;border-top:1px solid #e7edf5;color:#172033;\">"
                + esc(String.valueOf(value)) + "</td>"
                + "</tr>";
    }

    private String getEnv(String primaryName, String fallbackName) {
        String value = System.getenv(primaryName);
        if (isBlank(value)) {
            value = System.getenv(fallbackName);
        }
        return value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private String displayName(String fullName) {
        return isBlank(fullName) ? "Citizen" : fullName;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
