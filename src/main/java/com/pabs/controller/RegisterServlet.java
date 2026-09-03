package com.pabs.controller;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pabs.dao.EmailNotificationDAO;
import com.pabs.dao.UserDAO;
import com.pabs.model.User;
import com.pabs.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());

    private final UserDAO userDAO = new UserDAO();
    private final EmailNotificationDAO emailNotificationDAO = new EmailNotificationDAO();
    private final EmailService emailService = new EmailService();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        User user = new User(fullName, email, password);

        boolean success = userDAO.registerUser(user);

        if (success) {
            boolean emailSent = sendRegistrationSuccessEmail(user);
            HttpSession session = request.getSession();
            session.setAttribute("registrationEmailSent", emailSent);
            session.setAttribute("registeredEmail", user.getEmail());
            response.sendRedirect("register-success.jsp");
        } else {
            response.sendRedirect("register.jsp");
        }
    }

    private boolean sendRegistrationSuccessEmail(User user) {
        try {
            emailService.sendRegistrationSuccessEmail(user.getEmail(), user.getFullName());
            emailNotificationDAO.record(null, null, null, "REGISTRATION_SUCCESS",
                    user.getEmail(), "Passport Appointment Booking System - Registration Successful",
                    true, null);
            LOGGER.info(() -> "Registration confirmation email sent. registeredEmail=" + user.getEmail());
            return true;
        } catch (MessagingException e) {
            emailNotificationDAO.record(null, null, null, "REGISTRATION_SUCCESS",
                    user.getEmail(), "Passport Appointment Booking System - Registration Successful",
                    false, e.getMessage());
            LOGGER.log(Level.WARNING,
                    "Registration confirmation email failed. registeredEmail=" + user.getEmail()
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            return false;
        }
    }
}
