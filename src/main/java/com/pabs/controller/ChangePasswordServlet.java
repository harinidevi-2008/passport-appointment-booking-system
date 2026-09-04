package com.pabs.controller;

import java.io.IOException;

import com.pabs.dao.UserDAO;
import com.pabs.model.User;
import com.pabs.util.CsrfUtil;
import com.pabs.util.PasswordPolicy;
import com.pabs.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/change-password")
public class ChangePasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isAuthenticated(session)) {
            response.sendRedirect("login.jsp");
            return;
        }

        request.getRequestDispatcher("change-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isAuthenticated(session)) {
            response.sendRedirect("login.jsp");
            return;
        }

        if (!CsrfUtil.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        User user = userDAO.findById(userId);
        if (user == null) {
            session.invalidate();
            response.sendRedirect("login.jsp");
            return;
        }

        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        String validationError = validatePasswordChange(user, currentPassword, newPassword, confirmPassword);
        if (validationError != null) {
            request.setAttribute("errorMessage", validationError);
            request.getRequestDispatcher("change-password.jsp").forward(request, response);
            return;
        }

        if (!userDAO.updatePassword(userId, newPassword)) {
            request.setAttribute("errorMessage", "Unable to update your password. Please try again.");
            request.getRequestDispatcher("change-password.jsp").forward(request, response);
            return;
        }

        response.sendRedirect("change-password?updated=1");
    }

    private String validatePasswordChange(User user,
                                          String currentPassword,
                                          String newPassword,
                                          String confirmPassword) {
        if (isBlank(currentPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            return "Current password, new password, and confirmation are required.";
        }

        if (!PasswordUtil.verifyPassword(currentPassword, user.getPassword())) {
            return "Current password is incorrect.";
        }

        String passwordPolicyError = PasswordPolicy.validateNewPassword(newPassword, confirmPassword);
        if (passwordPolicyError != null) {
            return passwordPolicyError;
        }

        if (PasswordUtil.verifyPassword(newPassword, user.getPassword())) {
            return "New password must be different from the current password.";
        }

        return null;
    }

    private boolean isAuthenticated(HttpSession session) {
        return session != null && session.getAttribute("userId") != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
