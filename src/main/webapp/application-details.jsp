<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
                <span class="badge text-bg-success status-badge"><%= value(passportApplication.getStatus()) %></span>
            </div>

            <% if ("REJECTED".equals(passportApplication.getStatus())
                    && passportApplication.getReviewNote() != null
                    && !passportApplication.getReviewNote().trim().isEmpty()) { %>
                <div class="alert alert-danger" role="alert">
                    <strong>Rejection Reason:</strong> <%= value(passportApplication.getReviewNote()) %>
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

            <div class="d-flex flex-column flex-sm-row gap-2 mt-4">
                <a class="btn btn-primary" href="my-applications">My Applications</a>
                <a class="btn btn-outline-secondary" href="user-dashboard.jsp">Back to Dashboard</a>
            </div>
        </div>
    </div>
</main>
</body>
</html>
