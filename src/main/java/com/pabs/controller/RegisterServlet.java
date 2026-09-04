package com.pabs.controller;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pabs.dao.EmailNotificationDAO;
import com.pabs.dao.UserDAO;
import com.pabs.model.User;
import com.pabs.service.EmailService;
import com.pabs.util.PasswordPolicy;

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

        String fullName = clean(request.getParameter("fullName"));
        String email = clean(request.getParameter("email")).toLowerCase();
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        String validationError = validateRegistration(fullName, email, password, confirmPassword);
        if (validationError != null) {
            request.setAttribute("errorMessage", validationError);
            request.setAttribute("fullName", fullName);
            request.setAttribute("email", email);
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        User user = new User(fullName, email, password);

        boolean success = userDAO.registerUser(user);

        if (success) {
            boolean emailSent = sendRegistrationSuccessEmail(user);
            HttpSession session = request.getSession();
            session.setAttribute("registrationEmailSent", emailSent);
            session.setAttribute("registeredEmail", user.getEmail());
            response.sendRedirect("register-success.jsp");
        } else {
            request.setAttribute("errorMessage", "Unable to create the account. The email may already be registered.");
            request.setAttribute("fullName", fullName);
            request.setAttribute("email", email);
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }

    private String validateRegistration(String fullName, String email, String password, String confirmPassword) {
        if (fullName.isEmpty() || email.isEmpty()) {
            return "Full name and email are required.";
        }
        if (fullName.length() > 100) {
            return "Full name must be 100 characters or fewer.";
        }
        if (email.length() > 100 || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return "Please enter a valid email address.";
        }
        if (userDAO.findByEmail(email) != null) {
            return "This email address is already registered.";
        }
        return PasswordPolicy.validateNewPassword(password, confirmPassword);
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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
