package com.pabs.service;

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

    private final String username;
    private final String password;
    private final Session session;

    public EmailService() {
        this.username = System.getenv("PABS_MAIL_USERNAME");
        this.password = System.getenv("PABS_MAIL_PASSWORD");

        String host = getEnvOrDefault("PABS_MAIL_HOST", DEFAULT_HOST);
        String port = getEnvOrDefault("PABS_MAIL_PORT", DEFAULT_PORT);

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

    public void sendEmail(String to, String subject, String body) throws MessagingException {
        if (isBlank(username) || isBlank(password)) {
            throw new MessagingException("Mail credentials are not configured.");
        }

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    public void sendPasswordResetOtp(String to, String otp) throws MessagingException {
        String subject = "Passport Appointment Booking System - Password Reset OTP";
        String body = "Passport Appointment Booking System\n\n"
                + "Your password reset OTP is:\n\n"
                + otp + "\n\n"
                + "This OTP is valid for 5 minutes.\n\n"
                + "If you did not request a password reset, please ignore this email.";

        sendEmail(to, subject, body);
    }

    public void sendAppointmentRescheduled(
            String to,
            String appointmentNumber,
            String applicationNumber,
            String officeName,
            String oldSchedule,
            String newSchedule) throws MessagingException {

        String subject = "Passport Appointment Booking System - Appointment Rescheduled";
        String body = "Passport Appointment Booking System\n\n"
                + "Your appointment has been rescheduled successfully.\n\n"
                + "Appointment Number: " + appointmentNumber + "\n"
                + "Application Number: " + applicationNumber + "\n"
                + "Office: " + officeName + "\n"
                + "Previous Schedule: " + oldSchedule + "\n"
                + "New Schedule: " + newSchedule + "\n\n"
                + "Please log in to PABS to view your latest appointment details.";

        sendEmail(to, subject, body);
    }

    public void sendAppointmentBookedEmail(
            String to,
            String fullName,
            String appointmentNumber,
            String applicationNumber,
            String officeName,
            String appointmentDate,
            String startTime,
            String endTime,
            String status) throws MessagingException {

        String subject = "Passport Appointment Booking System - Appointment Booked";
        String body = "Passport Appointment Booking System\n\n"
                + "Dear " + fullName + ",\n\n"
                + "Your appointment was successfully booked.\n\n"
                + "Appointment Number: " + appointmentNumber + "\n"
                + "Application Number: " + applicationNumber + "\n"
                + "Office: " + officeName + "\n"
                + "Appointment Date: " + appointmentDate + "\n"
                + "Appointment Time: " + startTime + " - " + endTime + "\n"
                + "Current Status: " + status + "\n\n"
                + "You can view this appointment under My Appointments after logging in to PABS.\n\n"
                + "Thank you.";

        sendEmail(to, subject, body);
    }

    public void sendAppointmentCancelledEmail(
            String to,
            String fullName,
            String appointmentNumber,
            String applicationNumber,
            String officeName,
            String appointmentDate,
            String startTime,
            String endTime) throws MessagingException {

        String subject = "Passport Appointment Booking System - Appointment Cancelled";
        String body = "Passport Appointment Booking System\n\n"
                + "Dear " + fullName + ",\n\n"
                + "Your appointment was cancelled successfully.\n\n"
                + "Appointment Number: " + appointmentNumber + "\n"
                + "Application Number: " + applicationNumber + "\n"
                + "Office: " + officeName + "\n"
                + "Original Appointment Date: " + appointmentDate + "\n"
                + "Original Appointment Time: " + startTime + " - " + endTime + "\n"
                + "Current Status: CANCELLED\n\n"
                + "The appointment slot has been released for other applicants.\n\n"
                + "Thank you.";

        sendEmail(to, subject, body);
    }

    public void sendApplicationSubmittedEmail(
            String to,
            String fullName,
            String applicationNumber,
            String status) throws MessagingException {

        String subject = "Passport Application Submitted Successfully";
        String body = "Passport Appointment Booking System\n\n"
                + "Dear " + fullName + ",\n\n"
                + "Your passport application was successfully submitted.\n\n"
                + "Application Number: " + applicationNumber + "\n"
                + "Current Status: " + status + "\n\n"
                + "You can track this application from My Applications after logging in to PABS.\n\n"
                + "Thank you.";

        sendEmail(to, subject, body);
    }

    public void sendRegistrationSuccessEmail(
            String to,
            String fullName) throws MessagingException {

        String subject = "Passport Appointment Booking System - Registration Successful";
        String body = "Passport Appointment Booking System\n\n"
                + "Dear " + fullName + ",\n\n"
                + "This is a confirmation that your PABS account was successfully created.\n\n"
                + "Registered Email: " + to + "\n\n"
                + "You can now log in and use the Passport Appointment Booking System.\n\n"
                + "Thank you.";

        sendEmail(to, subject, body);
    }

    private String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
