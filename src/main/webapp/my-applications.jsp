<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.pabs.model.PassportApplication" %>
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

    List<PassportApplication> applications =
            (List<PassportApplication>) request.getAttribute("applications");
    if (applications == null) {
        response.sendRedirect("my-applications");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My Applications</title>
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
            <a class="nav-link" href="appointment?action=my">My Appointments</a>
            <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="dashboard-panel table-panel">
            <div class="d-flex flex-column flex-md-row justify-content-between gap-3 mb-4">
                <div>
                    <p class="portal-kicker mb-2">Applications</p>
                    <h1 class="h3 mb-0">My Applications</h1>
                </div>
                <a class="btn btn-primary align-self-md-start" href="passport-application">Apply for Passport</a>
            </div>

            <% if (applications.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-3">You have not submitted any passport applications yet.</p>
                    <a class="btn btn-primary" href="passport-application">Apply for Passport</a>
                </div>
            <% } else { %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle application-table">
                        <thead>
                        <tr>
                            <th>Application Number</th>
                            <th>Application Type</th>
                            <th>Mode</th>
                            <th>Status</th>
                            <th>Created Date</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (PassportApplication item : applications) { %>
                            <tr>
                                <td><strong><%= value(item.getApplicationNumber()) %></strong></td>
                                <td><%= value(item.getApplicationType()) %></td>
                                <td><%= value(item.getPassportMode()) %></td>
                                <td><span class="badge text-bg-success"><%= value(item.getStatus()) %></span></td>
                                <td><%= value(item.getCreatedAt()) %></td>
                                <td>
                                    <div class="d-flex flex-wrap gap-2">
                                        <a class="btn btn-sm btn-primary" href="application-details?id=<%= item.getId() %>">View Details</a>
                                        <a class="btn btn-sm btn-outline-primary" href="appointment?action=book&applicationId=<%= item.getId() %>">Book Appointment</a>
                                    </div>
                                </td>
                            </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            <% } %>

            <a class="btn btn-outline-secondary mt-3" href="user-dashboard.jsp">Back to Dashboard</a>
        </div>
    </div>
</main>
</body>
</html>
