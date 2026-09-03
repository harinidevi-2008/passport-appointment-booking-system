<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pabs.controller.AppointmentServlet" %>
<%@ page import="com.pabs.model.Appointment" %>
<%@ page import="com.pabs.model.PassportApplication" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%!
    private String value(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String applicationStatusClass(String status) {
        if ("REJECTED".equals(status)) {
            return "text-bg-danger";
        }
        if ("VERIFIED".equals(status) || "APPROVED".equals(status) || "PROCESSING".equals(status)
                || "PRINTING".equals(status) || "DISPATCHED".equals(status) || "DELIVERED".equals(status)) {
            return "text-bg-success";
        }
        if ("UNDER_REVIEW".equals(status)) {
            return "text-bg-warning";
        }
        if ("SUBMITTED".equals(status)) {
            return "text-bg-info";
        }
        if ("CANCELLED".equals(status)) {
            return "text-bg-secondary";
        }
        return "text-bg-secondary";
    }
%>
<%
    if (session == null || session.getAttribute("userId") == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    String role = (String) session.getAttribute("role");
    if (!"USER".equalsIgnoreCase(role)) {
        response.sendRedirect("admin-dashboard.jsp");
        return;
    }

    PassportApplication passportApplication =
            (PassportApplication) request.getAttribute("application");
    if (passportApplication == null) {
        response.sendRedirect("my-applications");
        return;
    }

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    String dob = passportApplication.getDateOfBirth() == null
            ? ""
            : passportApplication.getDateOfBirth().format(dateFormatter);
    String submittedOn = passportApplication.getCreatedAt() == null
            ? ""
            : passportApplication.getCreatedAt().toLocalDateTime().format(dateFormatter);
    Appointment activeAppointment = (Appointment) request.getAttribute("activeAppointment");
    Appointment latestAppointment = (Appointment) request.getAttribute("latestAppointment");
    Boolean canBookAppointmentAttribute = (Boolean) request.getAttribute("canBookAppointment");
    boolean canBookAppointment = Boolean.TRUE.equals(canBookAppointmentAttribute);
    Boolean hasCompletedAppointmentAttribute = (Boolean) request.getAttribute("hasCompletedAppointment");
    boolean hasCompletedAppointment = Boolean.TRUE.equals(hasCompletedAppointmentAttribute);
    Boolean canWithdrawApplicationAttribute = (Boolean) request.getAttribute("canWithdrawApplication");
    boolean canWithdrawApplication = Boolean.TRUE.equals(canWithdrawApplicationAttribute);
    boolean latestNoShow = latestAppointment != null && "NO_SHOW".equals(latestAppointment.getStatus());
    boolean showingWithdrawConfirmation = "1".equals(request.getParameter("confirmWithdraw"));
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Application Details</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg user-navbar">
    <div class="container">
        <a class="navbar-brand" href="user-dashboard.jsp">Passport Appointment Booking System</a>
        <div class="user-nav-links">
            <a class="nav-link" href="user-dashboard.jsp">Dashboard</a>
            <a class="nav-link" href="passport-application">Apply Passport</a>
            <a class="nav-link active" href="my-applications">My Applications</a>
            <a class="nav-link" href="profile">My Profile</a>
            <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="details-shell">
            <div class="details-header">
                <div>
                    <p class="portal-kicker mb-2">Application Details</p>
                    <h1>Application Number</h1>
                    <p class="mb-0"><%= value(passportApplication.getApplicationNumber()) %></p>
                </div>
                <span class="badge <%= applicationStatusClass(passportApplication.getStatus()) %> status-badge"><%= value(passportApplication.getStatus()) %></span>
            </div>

            <% if ("REJECTED".equals(passportApplication.getStatus())
                    && passportApplication.getReviewNote() != null
                    && !passportApplication.getReviewNote().trim().isEmpty()) { %>
                <div class="alert alert-danger" role="alert">
                    <strong>Rejection Reason:</strong> <%= value(passportApplication.getReviewNote()) %>
                </div>
            <% } %>

            <% if (latestNoShow && canBookAppointment) { %>
                <div class="alert alert-info" role="alert">
                    Your previous appointment was marked as No Show. Your application remains verified and you may book a new appointment.
                </div>
            <% } %>

            <% if ("1".equals(request.getParameter("withdrawn"))) { %>
                <div class="alert alert-success" role="alert">
                    Your application has been withdrawn. Any active appointment has been cancelled and previous appointment history was retained.
                </div>
            <% } %>

            <% if ("1".equals(request.getParameter("mailError"))) { %>
                <div class="alert alert-warning" role="alert">
                    The application was withdrawn, but the confirmation email could not be sent.
                </div>
            <% } %>

            <% if ("1".equals(request.getParameter("withdrawError"))) { %>
                <div class="alert alert-danger" role="alert">
                    This application cannot be withdrawn.
                </div>
            <% } %>

            <div class="details-section">
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Application Number</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getApplicationNumber()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Application Type</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getApplicationType()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Passport Mode</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getPassportMode()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Processing Status</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getStatus()) %></div>
                </div>
            </div>

            <div class="details-section">
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Full Name</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getFullName()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">DOB</div>
                    <div class="col-md-8 detail-value"><%= value(dob) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Gender</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getGender()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Place of Birth</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getPlaceOfBirth()) %></div>
                </div>
            </div>

            <div class="details-section">
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Father</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getFatherName()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Mother</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getMotherName()) %></div>
                </div>
            </div>

            <div class="details-section">
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Phone</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getPhone()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Email</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getEmail()) %></div>
                </div>
            </div>

            <div class="details-section">
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Address</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getAddress()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">City</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getCity()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">State</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getState()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Pincode</div>
                    <div class="col-md-8 detail-value"><%= value(passportApplication.getPincode()) %></div>
                </div>
            </div>

            <div class="details-section mb-0">
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Submitted On</div>
                    <div class="col-md-8 detail-value"><%= value(submittedOn) %></div>
                </div>
            </div>

            <div class="details-section mt-4">
                <h2 class="h5 mb-3">Application Actions</h2>
                <% if (showingWithdrawConfirmation && canWithdrawApplication) { %>
                    <div class="alert alert-warning" role="alert">
                        <p class="mb-2"><strong>Are you sure you want to withdraw this application?</strong></p>
                        <p class="mb-0">
                            Withdrawing this application will cancel the application and any active appointment associated with it.
                            Previous appointment history will be retained.
                        </p>
                    </div>
                    <form action="application-details" method="post" class="d-flex flex-column flex-sm-row gap-2">
                        <input type="hidden" name="action" value="withdraw">
                        <input type="hidden" name="applicationId" value="<%= passportApplication.getId() %>">
                        <button type="submit" class="btn btn-danger">Confirm Withdrawal</button>
                        <a class="btn btn-outline-secondary" href="application-details?id=<%= passportApplication.getId() %>">Keep Application</a>
                    </form>
                <% } else { %>
                    <div class="d-flex flex-column flex-sm-row gap-2">
                        <% if (canBookAppointment) { %>
                            <a class="btn btn-outline-primary" href="appointment?action=book&applicationId=<%= passportApplication.getId() %>">Book Appointment</a>
                        <% } else if (AppointmentServlet.isEligibleForAppointmentBooking(passportApplication) && activeAppointment != null) { %>
                            <span class="appointment-note align-self-sm-center">Active appointment: <%= value(activeAppointment.getStatus()) %></span>
                        <% } else if (AppointmentServlet.isEligibleForAppointmentBooking(passportApplication) && hasCompletedAppointment) { %>
                            <span class="appointment-note align-self-sm-center">Appointment completed. Application is in post-appointment processing.</span>
                        <% } else { %>
                            <span class="appointment-note align-self-sm-center"><%= value(AppointmentServlet.appointmentEligibilityMessage(passportApplication)) %></span>
                        <% } %>
                        <% if (canWithdrawApplication) { %>
                            <a class="btn btn-outline-danger" href="application-details?id=<%= passportApplication.getId() %>&confirmWithdraw=1">Withdraw Application</a>
                        <% } else if ("CANCELLED".equals(passportApplication.getStatus())
                                && "Citizen withdrew application".equals(passportApplication.getReviewNote())) { %>
                            <span class="appointment-note align-self-sm-center">Application withdrawn by citizen.</span>
                        <% } %>
                    </div>
                <% } %>
            </div>

            <div class="d-flex flex-column flex-sm-row gap-2 mt-4">
                <a class="btn btn-primary" href="my-applications">My Applications</a>
                <a class="btn btn-outline-secondary" href="user-dashboard.jsp">Back to Dashboard</a>
            </div>
        </div>
    </div>
</main>
</body>
</html>
