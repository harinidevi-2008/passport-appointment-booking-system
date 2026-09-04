package com.pabs.controller;

import java.io.IOException;

import com.pabs.dao.UserDAO;
import com.pabs.model.User;
import com.pabs.util.CsrfUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String email = clean(request.getParameter("email")).toLowerCase();
        String password = request.getParameter("password");

        User user = userDAO.loginUser(email, password);

        if (user != null) {
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("role", user.getRole());
            CsrfUtil.rotateToken(session);

            if ("ADMIN".equalsIgnoreCase(user.getRole())) {

                response.sendRedirect("admin-dashboard.jsp");

            } else {

                response.sendRedirect("user-dashboard.jsp");

            }

        } else {

            response.sendRedirect("login.jsp?error=1");

        }

    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

}
