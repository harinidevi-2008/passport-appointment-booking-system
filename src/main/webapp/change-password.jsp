<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pabs.util.CsrfUtil" %>
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
    boolean admin = "ADMIN".equalsIgnoreCase(role);
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Change Password</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg <%= admin ? "navbar-dark dashboard-nav" : "user-navbar" %>">
    <div class="container">
        <a class="navbar-brand" href="<%= admin ? "admin-dashboard.jsp" : "user-dashboard.jsp" %>">Passport Appointment Booking System</a>
        <div class="<%= admin ? "d-flex gap-2 flex-wrap" : "user-nav-links" %>">
            <% if (admin) { %>
                <a class="btn btn-outline-light" href="admin-dashboard.jsp">Dashboard</a>
                <a class="btn btn-outline-light" href="admin-applications">Applications</a>
                <a class="btn btn-outline-light" href="admin-appointments">Appointments</a>
                <a class="btn btn-outline-light" href="admin-slots">Slot Management</a>
                <a class="btn btn-outline-light" href="admin-reports">Reports</a>
                <a class="btn btn-light" href="change-password">Change Password</a>
                <a class="btn btn-outline-light" href="logout">Logout</a>
            <% } else { %>
                <a class="nav-link" href="user-dashboard.jsp">Dashboard</a>
                <a class="nav-link" href="passport-application">Apply Passport</a>
                <a class="nav-link" href="my-applications">My Applications</a>
                <a class="nav-link" href="appointment?action=book">Smart Recommendations</a>
                <a class="nav-link" href="appointment?action=my">My Appointments</a>
                <a class="nav-link" href="profile">My Profile</a>
                <a class="nav-link active" href="change-password">Change Password</a>
                <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
            <% } %>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="details-shell profile-shell">
            <div class="details-header">
                <div>
                    <p class="portal-kicker mb-2">Account Security</p>
                    <h1>Change Password</h1>
                    <p class="mb-0">Update the password for your current authenticated account.</p>
                </div>
            </div>

            <% if ("1".equals(request.getParameter("updated"))) { %>
                <div class="alert alert-success" role="alert">Your password was updated successfully.</div>
            <% } %>
            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert"><%= value(request.getAttribute("errorMessage")) %></div>
            <% } %>

            <form action="change-password" method="post" class="needs-validation" novalidate>
                <input type="hidden" name="csrfToken" value="<%= CsrfUtil.getToken(session) %>">
                <section class="card form-section-card">
                    <div class="card-body">
                        <h2>Password</h2>
                        <div class="mb-3">
                            <label for="currentPassword" class="form-label">Current Password</label>
                            <input type="password" class="form-control" id="currentPassword" name="currentPassword" required>
                        </div>
                        <div class="mb-3">
                            <label for="newPassword" class="form-label">New Password</label>
                            <input type="password" class="form-control" id="newPassword" name="newPassword" minlength="8" required>
                        </div>
                        <div class="mb-0">
                            <label for="confirmPassword" class="form-label">Confirm New Password</label>
                            <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" minlength="8" required>
                        </div>
                    </div>
                </section>
                <div class="form-actions">
                    <a class="btn btn-outline-secondary" href="<%= admin ? "admin-dashboard.jsp" : "profile" %>">Cancel</a>
                    <button type="submit" class="btn btn-primary">Change Password</button>
                </div>
            </form>
        </div>
    </div>
</main>
</body>
</html>
