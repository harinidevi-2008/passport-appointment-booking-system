<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pabs.controller.AppointmentServlet.AppointmentView" %>
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
    <title>Appointment Details</title>
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
        <div class="details-shell">
            <div class="details-header">
                <div>
                    <p class="portal-kicker mb-2">Appointment Details</p>
                    <h1><%= appointmentView.getAppointment().getAppointmentNumber() %></h1>
                    <p>Passport Appointment Booking System</p>
                </div>
                <span class="badge text-bg-primary status-badge"><%= appointmentView.getAppointment().getStatus() %></span>
            </div>

            <% if ("1".equals(request.getParameter("rescheduled"))) { %>
                <div class="alert alert-success" role="alert">
                    Your appointment has been rescheduled successfully.
                </div>
            <% } %>

            <% if ("1".equals(request.getParameter("mailError"))) { %>
                <div class="alert alert-warning" role="alert">
                    Appointment was rescheduled, but the email notification could not be sent.
                </div>
            <% } %>

            <div class="details-section">
                <h2 class="h5 mb-3">Application</h2>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Application Number</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getApplication().getApplicationNumber() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Application Type</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getApplication().getApplicationType() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Passport Mode</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getApplication().getPassportMode() %></div>
                </div>
            </div>

            <div class="details-section">
                <h2 class="h5 mb-3">Office and Schedule</h2>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Office</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getOffice().getOfficeName() %> (<%= appointmentView.getOffice().getOfficeType() %>)</div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Address</div>
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
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Booked At</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getAppointment().getBookedAt() %></div>
                </div>
            </div>

            <div class="form-actions">
                <a class="btn btn-outline-secondary" href="appointment?action=my">Back to My Appointments</a>
                <% if (appointmentView.isReschedulable()) { %>
                    <a class="btn btn-primary" href="appointment?action=reschedule&appointmentId=<%= appointmentView.getAppointment().getId() %>">Reschedule Appointment</a>
                <% } %>
                <% if (appointmentView.isCancellable()) { %>
                    <form action="appointment?action=cancel" method="post" class="m-0">
                        <input type="hidden" name="appointmentId" value="<%= appointmentView.getAppointment().getId() %>">
                        <button type="submit" class="btn btn-outline-danger">Cancel Appointment</button>
                    </form>
                <% } %>
            </div>
        </div>
    </div>
</main>
</body>
</html>
