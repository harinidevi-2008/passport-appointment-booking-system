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

    private String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
