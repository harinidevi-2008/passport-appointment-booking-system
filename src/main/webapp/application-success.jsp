<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    private String applicationStatusClass(Object statusValue) {
        String status = statusValue == null ? "" : String.valueOf(statusValue);
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

    Object applicationId = session.getAttribute("lastApplicationId");
    Object applicationNumber = session.getAttribute("lastApplicationNumber");
    Object applicationStatus = session.getAttribute("lastApplicationStatus");
    Object applicationEmailSent = session.getAttribute("lastApplicationEmailSent");

    if (applicationId == null || applicationNumber == null) {
        response.sendRedirect("my-applications");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Application Submitted</title>
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
            <a class="nav-link" href="my-applications">My Applications</a>
            <a class="nav-link" href="profile">My Profile</a>
            <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="dashboard-panel success-panel text-center">
            <p class="text-uppercase text-muted fw-semibold mb-2">Application Submitted</p>
            <h1 class="h3 mb-3">Passport Application Submitted Successfully</h1>
            <p class="lead mb-1">Application Number: <strong><%= applicationNumber %></strong></p>
            <p class="mb-4">Status: <span class="badge <%= applicationStatusClass(applicationStatus) %>"><%= applicationStatus %></span></p>

            <% if (Boolean.TRUE.equals(applicationEmailSent)) { %>
                <div class="alert alert-success text-start" role="alert">
                    Your passport application has been submitted successfully. A confirmation email has been sent to your registered email address.
                </div>
            <% } else { %>
                <div class="alert alert-warning text-start" role="alert">
                    Your application was submitted successfully, but the confirmation email could not be sent. You can still track your application under My Applications.
                </div>
            <% } %>

            <div class="d-flex flex-column flex-sm-row gap-2 justify-content-center">
                <a class="btn btn-primary" href="application-details?id=<%= applicationId %>">View Application</a>
                <a class="btn btn-outline-primary" href="my-applications">My Applications</a>
                <a class="btn btn-outline-secondary" href="user-dashboard.jsp">Back to Dashboard</a>
            </div>
        </div>
    </div>
</main>
</body>
</html>
