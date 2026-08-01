package com.pabs.controller;

import java.io.IOException;

import com.pabs.dao.UserDAO;
import com.pabs.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

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
            response.sendRedirect("register-success.jsp");
        } else {
            response.sendRedirect("register.jsp");
        }
    }
}