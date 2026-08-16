<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pabs.controller.AdminAppointmentServlet.AdminAppointmentView" %>
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

    private String statusClass(String status) {
        if ("BOOKED".equals(status)) {
            return "text-bg-success";
        }
        if ("RESCHEDULED".equals(status)) {
            return "text-bg-primary";
        }
        if ("CANCELLED".equals(status)) {
            return "text-bg-warning";
        }
        if ("EXPIRED".equals(status)) {
            return "text-bg-secondary";
        }
        return "text-bg-dark";
    }
%>
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

    AdminAppointmentView appointmentView =
            (AdminAppointmentView) request.getAttribute("appointmentView");
    if (appointmentView == null) {
        response.sendRedirect("admin-appointments");
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
<nav class="navbar navbar-expand-lg navbar-dark dashboard-nav">
    <div class="container">
        <a class="navbar-brand" href="admin-dashboard.jsp">Passport Appointment Booking System</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-light" href="admin-appointments">Appointment Management</a>
            <a class="btn btn-outline-light" href="admin-applications">Application Processing</a>
            <a class="btn btn-outline-light" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page">
    <div class="container">
        <div class="details-shell">
            <div class="details-header">
                <div>
                    <p class="text-uppercase text-muted fw-semibold mb-2">Appointment Details</p>
                    <h1><%= value(appointmentView.getAppointment().getAppointmentNumber()) %></h1>
                    <p class="mb-0"><%= value(appointmentView.getApplication().getApplicationNumber()) %></p>
                </div>
                <span class="badge <%= statusClass(appointmentView.getAppointment().getStatus()) %> status-badge">
                    <%= value(appointmentView.getAppointment().getStatus()) %>
                </span>
            </div>

            <% if ("EXPIRED".equals(appointmentView.getAppointment().getStatus())) { %>
                <div class="alert alert-secondary" role="alert">
                    This appointment's scheduled end time has passed and it is retained as history.
                </div>
            <% } %>

            <div class="details-section">
                <h2 class="h5 mb-3">Appointment</h2>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Appointment Number</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getAppointment().getAppointmentNumber()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Status</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getAppointment().getStatus()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Created Date</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getAppointment().getCreatedAt()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Updated Date</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getAppointment().getUpdatedAt()) %></div>
                </div>
            </div>

            <div class="details-section">
                <h2 class="h5 mb-3">Applicant</h2>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Application Number</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getApplication().getApplicationNumber()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Applicant</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getApplication().getFullName()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Applicant Email</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getApplication().getEmail()) %></div>
                </div>
            </div>

            <div class="details-section mb-0">
                <h2 class="h5 mb-3">Office and Schedule</h2>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Office</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getOffice().getOfficeName()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Date</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getSlot().getAppointmentDate()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Start Time</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getSlot().getStartTime()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">End Time</div>
                    <div class="col-md-8 detail-value"><%= value(appointmentView.getSlot().getEndTime()) %></div>
                </div>
            </div>

            <div class="form-actions">
                <a class="btn btn-outline-secondary" href="admin-appointments">Back to Appointments</a>
            </div>
        </div>
    </div>
</main>
</body>
</html>
