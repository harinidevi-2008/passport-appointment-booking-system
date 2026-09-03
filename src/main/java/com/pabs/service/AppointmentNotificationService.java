package com.pabs.service;

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

import jakarta.mail.MessagingException;

public class AppointmentNotificationService {

    private static final Logger LOGGER = Logger.getLogger(AppointmentNotificationService.class.getName());

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final PassportApplicationDAO applicationDAO = new PassportApplicationDAO();
    private final AppointmentSlotDAO slotDAO = new AppointmentSlotDAO();
    private final PassportOfficeDAO officeDAO = new PassportOfficeDAO();
    private final EmailNotificationDAO emailNotificationDAO = new EmailNotificationDAO();
    private final EmailService emailService = new EmailService();

    public int expirePastAppointmentsAndNotifyNoShow() {
        List<Appointment> expiredAppointments = appointmentDAO.expirePastAppointmentsAndReturnExpired();
        for (Appointment appointment : expiredAppointments) {
            sendNoShowEmail(appointment);
        }
        return expiredAppointments.size();
    }

    private void sendNoShowEmail(Appointment appointment) {
        PassportApplication application = applicationDAO.getApplicationById(appointment.getApplicationId());
        AppointmentSlot slot = slotDAO.findById(appointment.getSlotId());
        PassportOffice office = slot == null ? null : officeDAO.findById(slot.getOfficeId());

        if (application == null || slot == null || office == null) {
            LOGGER.warning("NO_SHOW email skipped because appointment details could not be loaded. appointmentId="
                    + appointment.getId());
            return;
        }

        try {
            emailService.sendAppointmentNoShowEmail(
                    application.getEmail(),
                    application.getFullName(),
                    appointment.getAppointmentNumber(),
                    application.getApplicationNumber(),
                    office.getOfficeName(),
                    slot.getAppointmentDate(),
                    slot.getStartTime(),
                    slot.getEndTime()
            );
            recordEmail(appointment, application, "APPOINTMENT_NO_SHOW", application.getEmail(),
                    EmailService.SUBJECT_APPOINTMENT_NO_SHOW, true, null);
            LOGGER.info(() -> "NO_SHOW email sent. appointmentId=" + appointment.getId()
                    + ", appointmentNumber=" + appointment.getAppointmentNumber());
        } catch (MessagingException e) {
            recordEmail(appointment, application, "APPOINTMENT_NO_SHOW", application.getEmail(),
                    EmailService.SUBJECT_APPOINTMENT_NO_SHOW, false, e.getMessage());
            LOGGER.log(Level.WARNING,
                    "NO_SHOW email failed. appointmentId=" + appointment.getId()
                            + ", appointmentNumber=" + appointment.getAppointmentNumber()
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
        }
    }

    private void recordEmail(Appointment appointment,
                             PassportApplication application,
                             String notificationType,
                             String recipientEmail,
                             String subject,
                             boolean sent,
                             String errorMessage) {
        emailNotificationDAO.record(
                appointment.getUserId(),
                application.getId(),
                appointment.getId(),
                notificationType,
                recipientEmail,
                subject,
                sent,
                errorMessage
        );
    }
}
