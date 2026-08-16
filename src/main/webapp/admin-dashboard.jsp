<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    if (session == null || session.getAttribute("userId") == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    String role = (String) session.getAttribute("role");
    if (!"ADMIN".equalsIgnoreCase(role)) {
        response.sendRedirect("user-dashboard.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Admin Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark dashboard-nav">
    <div class="container">
        <span class="navbar-brand">Passport Appointment Booking System</span>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-light" href="admin-applications">Application Processing</a>
            <a class="btn btn-outline-light" href="admin-appointments">Appointment Management</a>
            <a class="btn btn-outline-light" href="admin-documents">Document Status</a>
            <a class="btn btn-outline-light" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page">
    <div class="container">
        <div class="dashboard-panel mb-4">
            <p class="text-uppercase text-muted fw-semibold mb-2">Admin Dashboard</p>
            <h1>Welcome, <%= session.getAttribute("fullName") %></h1>
            <p class="lead mb-0">You have administrator access.</p>
        </div>

        <div class="row g-4">
            <div class="col-md-4">
                <div class="service-card h-100">
                    <h2>Application Processing</h2>
                    <p>Review and process submitted passport applications.</p>
                    <a class="btn btn-primary mt-auto" href="admin-applications">Open Applications</a>
                </div>
            </div>
            <div class="col-md-4">
                <div class="service-card h-100">
                    <h2>Appointment Management</h2>
                    <p>View upcoming appointments and appointment history.</p>
                    <a class="btn btn-primary mt-auto" href="admin-appointments">Open Appointments</a>
                </div>
            </div>
            <div class="col-md-4">
                <div class="service-card h-100">
                    <h2>Document Review</h2>
                    <p>Review uploaded passport documents by citizen.</p>
                    <a class="btn btn-primary mt-auto" href="admin-documents">Open Documents</a>
                </div>
            </div>
        </div>
    </div>
</main>
</body>
</html>
