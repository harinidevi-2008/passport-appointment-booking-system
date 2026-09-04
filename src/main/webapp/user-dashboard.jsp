<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>User Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg user-navbar">
    <div class="container">
        <a class="navbar-brand" href="user-dashboard.jsp">Passport Appointment Booking System</a>
        <div class="user-nav-links">
            <a class="nav-link active" href="user-dashboard.jsp">Dashboard</a>
            <a class="nav-link" href="passport-application">Apply Passport</a>
            <a class="nav-link" href="my-applications">My Applications</a>
            <a class="nav-link" href="documents">Upload Documents</a>
            <a class="nav-link" href="appointment?action=book">Smart Recommendations</a>
            <a class="nav-link" href="appointment?action=my">My Appointments</a>
            <a class="nav-link" href="profile">My Profile</a>
            <a class="nav-link" href="change-password">Change Password</a>
            <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="portal-welcome-card mb-4">
            <p class="portal-kicker mb-2">User Dashboard</p>
            <h1>Welcome, <%= value(session.getAttribute("fullName")) %></h1>
            <p class="mb-0">Passport Appointment Booking System</p>
        </div>

        <div class="row g-4 align-items-stretch">
            <div class="col-lg-3">
                <div class="service-card h-100">
                    <div class="service-icon">🛂</div>
                    <h2>Apply for Passport</h2>
                    <p>Submit a new passport application.</p>
                    <a class="btn btn-primary btn-lg mt-auto" href="passport-application">Apply Now</a>
                </div>
            </div>
            <div class="col-lg-3">
                <div class="service-card h-100">
                    <div class="service-icon">📋</div>
                    <h2>My Applications</h2>
                    <p>View submitted applications and current status.</p>
                    <a class="btn btn-primary btn-lg mt-auto" href="my-applications">View Applications</a>
                </div>
            </div>
            <div class="col-lg-3">
                <div class="service-card h-100">
                    <div class="service-icon">D</div>
                    <h2>Documents</h2>
                    <p>Upload or replace required passport documents.</p>
                    <a class="btn btn-primary btn-lg mt-auto" href="documents">Upload Documents</a>
                </div>
            </div>
            <div class="col-lg-3">
                <div class="service-card h-100">
                    <div class="service-icon">A</div>
                    <h2>Smart Booking</h2>
                    <p>Find offices and slots using availability and crowd data.</p>
                    <a class="btn btn-primary btn-lg mt-auto" href="appointment?action=book">Find Slots</a>
                </div>
            </div>
            <div class="col-lg-3">
                <div class="service-card h-100">
                    <div class="service-icon">P</div>
                    <h2>My Profile</h2>
                    <p>View and update your account information.</p>
                    <a class="btn btn-primary btn-lg mt-auto" href="profile">Open Profile</a>
                </div>
            </div>
        </div>
    </div>
</main>
</body>
</html>
