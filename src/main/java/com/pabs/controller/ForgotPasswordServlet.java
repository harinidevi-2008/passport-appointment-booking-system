package com.pabs.controller;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final int OTP_REQUEST_COOLDOWN_SECONDS = 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordServlet.class.getName());

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();
    private final EmailService emailService = new EmailService();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String email = clean(request.getParameter("email"));
        HttpSession session = request.getSession();

        Long lastRequestTime = (Long) session.getAttribute("passwordResetLastRequestTime");
        long now = System.currentTimeMillis();
        if (lastRequestTime != null && now - lastRequestTime < OTP_REQUEST_COOLDOWN_SECONDS * 1000L) {
            request.setAttribute("errorMessage",
                    "Please wait a minute before requesting another OTP.");
            request.getRequestDispatcher("forgot-password.jsp").forward(request, response);
            return;
        }
        session.setAttribute("passwordResetLastRequestTime", now);

        clearResetState(session);

        if (!email.isEmpty()) {
            LOGGER.info(() -> "Password reset requested. emailProvided=true");
            User user = userDAO.findByEmail(email);
            LOGGER.info(() -> "Password reset user lookup result: found=" + (user != null));

            if (user != null) {
                String otp = generateOtp();
                int otpId = passwordResetDAO.createOtp(
                        user.getId(),
                        PasswordUtil.hashPassword(otp),
                        LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
                );
                LOGGER.info(() -> "Password reset OTP creation result: success="
                        + (otpId > 0) + ", otpId=" + otpId + ", userId=" + user.getId());

                if (otpId > 0) {
                    try {
                        emailService.sendPasswordResetOtp(user.getEmail(), otp);
                        LOGGER.info(() -> "Password reset OTP email sent successfully. otpId="
                                + otpId + ", userId=" + user.getId());

                        session.setAttribute("passwordResetUserId", user.getId());
                        session.setAttribute("passwordResetOtpId", otpId);
                        session.setAttribute("passwordResetVerified", Boolean.FALSE);
                    } catch (MessagingException e) {
                        passwordResetDAO.markUsed(otpId);
                        clearResetState(session);

                        LOGGER.log(Level.WARNING,
                                "Password reset OTP email send failed. otpId=" + otpId
                                        + ", userId=" + user.getId()
                                        + ", exceptionType=" + e.getClass().getName()
                                        + ", message=" + e.getMessage(),
                                e);

                        request.setAttribute("errorMessage",
                                "Unable to send the OTP email right now. Please check mail configuration or try again later.");
                        request.getRequestDispatcher("forgot-password.jsp").forward(request, response);
                        return;
                    }
                } else {
                    LOGGER.warning(() -> "Password reset OTP creation failed. userId=" + user.getId());
                }
            }
        } else {
            LOGGER.info(() -> "Password reset requested. emailProvided=false");
        }

        session.setAttribute("passwordResetMessage",
                "If the email address is registered, an OTP has been sent.");
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
