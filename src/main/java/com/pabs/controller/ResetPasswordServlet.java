package com.pabs.controller;

import java.io.IOException;

import com.pabs.dao.PasswordResetDAO;
import com.pabs.dao.UserDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!hasVerifiedReset(session)) {
            response.sendRedirect("forgot-password.jsp");
            return;
        }

        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String validationError = validatePassword(password, confirmPassword);

        if (validationError != null) {
            request.setAttribute("errorMessage", validationError);
            request.getRequestDispatcher("reset-password.jsp").forward(request, response);
            return;
        }

        int userId = (Integer) session.getAttribute("passwordResetUserId");
        int otpId = (Integer) session.getAttribute("passwordResetOtpId");

        if (userDAO.updatePassword(userId, password)) {
            passwordResetDAO.markUsed(otpId);
            clearResetState(session);
            session.setAttribute("loginSuccessMessage",
                    "Your password has been reset successfully. Please log in.");
            response.sendRedirect("login.jsp");
        } else {
            request.setAttribute("errorMessage", "Unable to reset password. Please try again.");
            request.getRequestDispatcher("reset-password.jsp").forward(request, response);
        }
    }

    private String validatePassword(String password, String confirmPassword) {
        if (password == null || password.trim().isEmpty()) {
            return "Please enter a new password.";
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least 8 characters long.";
        }

        if (!password.equals(confirmPassword)) {
            return "New password and confirmation password do not match.";
        }

        return null;
    }

    private boolean hasVerifiedReset(HttpSession session) {
        return session != null
                && session.getAttribute("passwordResetUserId") instanceof Integer
                && session.getAttribute("passwordResetOtpId") instanceof Integer
                && Boolean.TRUE.equals(session.getAttribute("passwordResetVerified"));
    }

    private void clearResetState(HttpSession session) {
        session.removeAttribute("passwordResetUserId");
        session.removeAttribute("passwordResetOtpId");
        session.removeAttribute("passwordResetVerified");
    }
}
