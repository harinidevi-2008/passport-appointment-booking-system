<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pabs.controller.AppointmentServlet.AppointmentView" %>
<%
    if (session == null || session.getAttribute("userId") == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    AppointmentView appointmentView = (AppointmentView) request.getAttribute("appointmentView");
    if (appointmentView == null) {
        response.sendRedirect("appointment?action=my");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Appointment Confirmed</title>
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
            <a class="nav-link active" href="appointment?action=my">My Appointments</a>
            <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="details-shell success-panel">
            <div class="details-header">
                <div>
                    <p class="portal-kicker mb-2">Appointment Confirmed</p>
                    <h1><%= appointmentView.getAppointment().getAppointmentNumber() %></h1>
                    <p>Your appointment has been booked successfully.</p>
                </div>
                <span class="badge text-bg-success status-badge"><%= appointmentView.getAppointment().getStatus() %></span>
            </div>

            <% if ("1".equals(request.getParameter("mailError"))) { %>
                <div class="alert alert-warning" role="alert">
                    Your appointment was booked successfully, but the confirmation email could not be sent.
                </div>
            <% } else { %>
                <div class="alert alert-success" role="alert">
                    Your appointment was booked successfully. A confirmation email has been sent to your registered email address.
                </div>
            <% } %>

            <div class="details-section">
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Application Number</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getApplication().getApplicationNumber() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Office</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getOffice().getOfficeName() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Office Address</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getOffice().getAddress() %>, <%= appointmentView.getOffice().getCity() %>, <%= appointmentView.getOffice().getState() %> - <%= appointmentView.getOffice().getPincode() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Date</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getSlot().getAppointmentDate() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Time</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getSlot().getStartTime() %> - <%= appointmentView.getSlot().getEndTime() %></div>
                </div>
            </div>

            <div class="form-actions">
                <a class="btn btn-outline-secondary" href="user-dashboard.jsp">Return to Dashboard</a>
                <a class="btn btn-primary" href="appointment?action=my">View My Appointments</a>
            </div>
        </div>
    </div>
</main>
</body>
</html>
