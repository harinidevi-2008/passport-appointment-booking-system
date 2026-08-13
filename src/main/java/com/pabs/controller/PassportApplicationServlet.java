package com.pabs.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.pabs.dao.PassportApplicationDAO;
import com.pabs.model.PassportApplication;
import com.pabs.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet({"/passport-application", "/my-applications", "/application-details"})
public class PassportApplicationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PassportApplicationServlet.class.getName());

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final Pattern PINCODE_PATTERN =
            Pattern.compile("^[0-9]{5,10}$");

    private final PassportApplicationDAO applicationDAO = new PassportApplicationDAO();
    private final EmailService emailService = new EmailService();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isLoggedInUser(session)) {
            redirectBySession(session, response);
            return;
        }

        String servletPath = request.getServletPath();

        if ("/passport-application".equals(servletPath)) {
            request.getRequestDispatcher("apply-passport.jsp").forward(request, response);
            return;
        }

        if ("/my-applications".equals(servletPath)) {
            int userId = (Integer) session.getAttribute("userId");
            List<PassportApplication> applications = applicationDAO.getApplicationsByUserId(userId);
            request.setAttribute("applications", applications);
            request.getRequestDispatcher("my-applications.jsp").forward(request, response);
            return;
        }

        if ("/application-details".equals(servletPath)) {
            showApplicationDetails(request, response, session);
        }
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

        if (!"/passport-application".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        PassportApplication application = buildApplication(request, session);
        String validationError = validateApplication(application);

        if (validationError != null) {
            request.setAttribute("errorMessage", validationError);
            request.setAttribute("application", application);
            request.getRequestDispatcher("apply-passport.jsp").forward(request, response);
            return;
        }

        boolean created = applicationDAO.createApplication(application);

        if (created) {
            boolean emailSent = sendApplicationSubmittedEmail(application);
            session.setAttribute("lastApplicationId", application.getId());
            session.setAttribute("lastApplicationNumber", application.getApplicationNumber());
            session.setAttribute("lastApplicationStatus", application.getStatus());
            session.setAttribute("lastApplicationEmailSent", emailSent);
            response.sendRedirect("application-success.jsp");
        } else {
            request.setAttribute("errorMessage", "Unable to submit your application. Please try again.");
            request.setAttribute("application", application);
            request.getRequestDispatcher("apply-passport.jsp").forward(request, response);
        }
    }

    private boolean sendApplicationSubmittedEmail(PassportApplication application) {
        try {
            emailService.sendApplicationSubmittedEmail(
                    application.getEmail(),
                    application.getFullName(),
                    application.getApplicationNumber(),
                    application.getStatus()
            );
            LOGGER.info(() -> "Application submission email sent. applicationId="
                    + application.getId() + ", applicationNumber=" + application.getApplicationNumber());
            return true;
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING,
                    "Application submission email failed. applicationId=" + application.getId()
                            + ", applicationNumber=" + application.getApplicationNumber()
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            return false;
        }
    }

    private void showApplicationDetails(HttpServletRequest request,
                                        HttpServletResponse response,
                                        HttpSession session)
            throws ServletException, IOException {

        int applicationId;
        try {
            applicationId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        PassportApplication application = applicationDAO.getApplicationById(applicationId);
        int userId = (Integer) session.getAttribute("userId");

        if (application == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (application.getUserId() != userId) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        request.setAttribute("application", application);
        request.getRequestDispatcher("application-details.jsp").forward(request, response);
    }

    private PassportApplication buildApplication(HttpServletRequest request, HttpSession session) {
        PassportApplication application = new PassportApplication();
        application.setUserId((Integer) session.getAttribute("userId"));
        application.setApplicationType(clean(request.getParameter("applicationType")));
        application.setPassportMode(clean(request.getParameter("passportMode")));
        application.setFullName(clean(request.getParameter("fullName")));
        application.setDateOfBirth(parseDate(request.getParameter("dateOfBirth")));
        application.setGender(clean(request.getParameter("gender")));
        application.setPlaceOfBirth(clean(request.getParameter("placeOfBirth")));
        application.setFatherName(clean(request.getParameter("fatherName")));
        application.setMotherName(clean(request.getParameter("motherName")));
        application.setPhone(clean(request.getParameter("phone")));
        application.setEmail(clean((String) session.getAttribute("email")));
        application.setAddress(clean(request.getParameter("address")));
        application.setCity(clean(request.getParameter("city")));
        application.setState(clean(request.getParameter("state")));
        application.setPincode(clean(request.getParameter("pincode")));
        application.setStatus("SUBMITTED");
        return application;
    }

    private String validateApplication(PassportApplication application) {
        if (isBlank(application.getApplicationType()) || isBlank(application.getPassportMode())
                || isBlank(application.getFullName()) || application.getDateOfBirth() == null
                || isBlank(application.getGender()) || isBlank(application.getPlaceOfBirth())
                || isBlank(application.getFatherName()) || isBlank(application.getMotherName())
                || isBlank(application.getPhone()) || isBlank(application.getEmail())
                || isBlank(application.getAddress()) || isBlank(application.getCity())
                || isBlank(application.getState()) || isBlank(application.getPincode())) {
            return "Please fill in all required fields.";
        }

        if (!"Fresh Passport".equals(application.getApplicationType())
                && !"Reissue".equals(application.getApplicationType())) {
            return "Please select a valid application type.";
        }

        if (!"Normal".equals(application.getPassportMode())
                && !"Tatkal".equals(application.getPassportMode())) {
            return "Please select a valid passport mode.";
        }

        if (!"Male".equals(application.getGender())
                && !"Female".equals(application.getGender())
                && !"Other".equals(application.getGender())) {
            return "Please select a valid gender.";
        }

        if (application.getDateOfBirth().isAfter(LocalDate.now())) {
            return "Please enter a valid date of birth.";
        }

        if (!EMAIL_PATTERN.matcher(application.getEmail()).matches()) {
            return "Please enter a valid email address.";
        }

        if (!PHONE_PATTERN.matcher(application.getPhone()).matches()) {
            return "Please enter a valid phone number.";
        }

        if (!PINCODE_PATTERN.matcher(application.getPincode()).matches()) {
            return "Please enter a valid pincode.";
        }

        return null;
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

    private LocalDate parseDate(String value) {
        try {
            return isBlank(value) ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
