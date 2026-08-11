package com.pabs.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

import com.pabs.dao.PasswordResetDAO;
import com.pabs.dao.PasswordResetDAO.PasswordResetOtpRecord;
import com.pabs.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/verify-otp")
public class VerifyOTPServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int MAX_ATTEMPTS = 5;
    private static final Pattern OTP_PATTERN = Pattern.compile("^[0-9]{6}$");

    private final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!hasResetRequest(session)) {
            response.sendRedirect("forgot-password.jsp");
            return;
        }

        String otp = clean(request.getParameter("otp"));
        int userId = (Integer) session.getAttribute("passwordResetUserId");
        int otpId = (Integer) session.getAttribute("passwordResetOtpId");

        Optional<PasswordResetOtpRecord> optionalRecord = passwordResetDAO.findOtp(otpId, userId);
        if (optionalRecord.isEmpty()) {
            clearResetState(session);
            response.sendRedirect("forgot-password.jsp");
            return;
        }

        PasswordResetOtpRecord record = optionalRecord.get();
        if (record.isUsed() || record.getAttempts() >= MAX_ATTEMPTS) {
            clearResetState(session);
            response.sendRedirect("forgot-password.jsp?expired=1");
            return;
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetDAO.markUsed(record.getId());
            clearResetState(session);
            response.sendRedirect("forgot-password.jsp?expired=1");
            return;
        }

        if (!OTP_PATTERN.matcher(otp).matches()
                || !PasswordUtil.verifyPassword(otp, record.getOtpHash())) {
            passwordResetDAO.incrementAttempts(record.getId());

            if (record.getAttempts() + 1 >= MAX_ATTEMPTS) {
                passwordResetDAO.markUsed(record.getId());
                clearResetState(session);
                response.sendRedirect("forgot-password.jsp?attempts=1");
                return;
            }

            request.setAttribute("errorMessage", "Invalid OTP. Please check the code and try again.");
            request.getRequestDispatcher("verify-otp.jsp").forward(request, response);
            return;
        }

        session.setAttribute("passwordResetVerified", Boolean.TRUE);
        response.sendRedirect("reset-password.jsp");
    }

    private boolean hasResetRequest(HttpSession session) {
        return session != null
                && session.getAttribute("passwordResetUserId") instanceof Integer
                && session.getAttribute("passwordResetOtpId") instanceof Integer;
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
