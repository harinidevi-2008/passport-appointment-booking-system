package com.pabs.controller;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

import com.pabs.dao.PasswordResetDAO;
import com.pabs.dao.UserDAO;
import com.pabs.model.User;
import com.pabs.service.EmailService;
import com.pabs.util.PasswordUtil;

import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();
    private final EmailService emailService = new EmailService();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String email = clean(request.getParameter("email"));
        HttpSession session = request.getSession();

        clearResetState(session);
        session.setAttribute("passwordResetMessage",
                "If the email address is registered, an OTP has been sent.");

        if (!email.isEmpty()) {
            User user = userDAO.findByEmail(email);

            if (user != null) {
                String otp = generateOtp();
                int otpId = passwordResetDAO.createOtp(
                        user.getId(),
                        PasswordUtil.hashPassword(otp),
                        LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
                );

                if (otpId > 0) {
                    session.setAttribute("passwordResetUserId", user.getId());
                    session.setAttribute("passwordResetOtpId", otpId);
                    session.setAttribute("passwordResetVerified", Boolean.FALSE);

                    try {
                        emailService.sendPasswordResetOtp(user.getEmail(), otp);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        response.sendRedirect("verify-otp.jsp");
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void clearResetState(HttpSession session) {
        session.removeAttribute("passwordResetUserId");
        session.removeAttribute("passwordResetOtpId");
        session.removeAttribute("passwordResetVerified");
    }
}
