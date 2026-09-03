package com.pabs.controller;

import java.io.IOException;
import java.util.regex.Pattern;

import com.pabs.dao.UserDAO;
import com.pabs.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isLoggedInUser(session)) {
            redirectBySession(session, response);
            return;
        }

        User user = currentUser(session);
        if (user == null) {
            session.invalidate();
            response.sendRedirect("login.jsp");
            return;
        }

        request.setAttribute("user", user);
        request.setAttribute("editMode", "edit".equals(clean(request.getParameter("mode"))));
        request.getRequestDispatcher("profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isLoggedInUser(session)) {
            redirectBySession(session, response);
            return;
        }

        User user = currentUser(session);
        if (user == null) {
            session.invalidate();
            response.sendRedirect("login.jsp");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String fullName = clean(request.getParameter("fullName"));
        String email = clean(request.getParameter("email")).toLowerCase();

        String validationError = validateProfile(userId, fullName, email);
        if (validationError != null) {
            user.setFullName(fullName);
            user.setEmail(email);
            request.setAttribute("user", user);
            request.setAttribute("editMode", Boolean.TRUE);
            request.setAttribute("errorMessage", validationError);
            request.getRequestDispatcher("profile.jsp").forward(request, response);
            return;
        }

        boolean updated = userDAO.updateProfile(userId, fullName, email);
        if (!updated) {
            request.setAttribute("user", user);
            request.setAttribute("editMode", Boolean.TRUE);
            request.setAttribute("errorMessage", "Unable to update your profile. Please try again.");
            request.getRequestDispatcher("profile.jsp").forward(request, response);
            return;
        }

        session.setAttribute("fullName", fullName);
        session.setAttribute("email", email);
        response.sendRedirect("profile?updated=1");
    }

    private String validateProfile(int userId, String fullName, String email) {
        if (isBlank(fullName) || isBlank(email)) {
            return "Full name and email are required.";
        }

        if (fullName.length() > 100) {
            return "Full name must be 100 characters or fewer.";
        }

        if (email.length() > 100 || !EMAIL_PATTERN.matcher(email).matches()) {
            return "Please enter a valid email address.";
        }

        if (userDAO.emailExistsForAnotherUser(email, userId)) {
            return "This email address is already registered to another account.";
        }

        return null;
    }

    private User currentUser(HttpSession session) {
        return userDAO.findById((Integer) session.getAttribute("userId"));
    }

    private boolean isLoggedInUser(HttpSession session) {
        return session != null
                && session.getAttribute("userId") != null
                && "USER".equalsIgnoreCase((String) session.getAttribute("role"));
    }

    private void redirectBySession(HttpSession session, HttpServletResponse response) throws IOException {
        if (session != null && session.getAttribute("userId") != null
                && "ADMIN".equalsIgnoreCase((String) session.getAttribute("role"))) {
            response.sendRedirect("admin-dashboard.jsp");
        } else {
            response.sendRedirect("login.jsp");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
