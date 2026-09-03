<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
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
        if ("ATTENDED".equals(status)) {
            return "text-bg-info";
        }
        if ("COMPLETED".equals(status)) {
            return "text-bg-dark";
        }
        if ("CANCELLED".equals(status)) {
            return "text-bg-warning";
        }
        if ("NO_SHOW".equals(status)) {
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

    List<AdminAppointmentView> appointmentViews =
            (List<AdminAppointmentView>) request.getAttribute("appointmentViews");
    if (appointmentViews == null) {
        response.sendRedirect("admin-appointments");
        return;
    }
    String selectedStatus = (String) request.getAttribute("selectedStatus");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Appointment Management</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark dashboard-nav">
    <div class="container">
        <a class="navbar-brand" href="admin-dashboard.jsp">Passport Appointment Booking System</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-light" href="admin-dashboard.jsp">Dashboard</a>
            <a class="btn btn-outline-light" href="admin-applications">Application Processing</a>
            <a class="btn btn-outline-light" href="admin-documents">Document Status</a>
            <a class="btn btn-outline-light" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page">
    <div class="container">
        <div class="dashboard-panel table-panel">
            <div class="d-flex flex-column flex-md-row justify-content-between gap-3 mb-4">
                <div>
                    <p class="text-uppercase text-muted fw-semibold mb-2">Admin</p>
                    <h1 class="h3 mb-0">Appointment Management</h1>
                </div>
                <a class="btn btn-outline-secondary align-self-md-start" href="admin-dashboard.jsp">Back to Dashboard</a>
            </div>

            <form action="admin-appointments" method="get" class="row g-2 mb-4">
                <div class="col-md-4">
                    <select class="form-select" name="status">
                        <option value="UPCOMING" <%= selectedStatus == null || "UPCOMING".equals(selectedStatus) ? "selected" : "" %>>Upcoming</option>
                        <option value="ALL" <%= "ALL".equals(selectedStatus) ? "selected" : "" %>>All</option>
                        <option value="BOOKED" <%= "BOOKED".equals(selectedStatus) ? "selected" : "" %>>BOOKED</option>
                        <option value="RESCHEDULED" <%= "RESCHEDULED".equals(selectedStatus) ? "selected" : "" %>>RESCHEDULED</option>
                        <option value="ATTENDED" <%= "ATTENDED".equals(selectedStatus) ? "selected" : "" %>>ATTENDED</option>
                        <option value="COMPLETED" <%= "COMPLETED".equals(selectedStatus) ? "selected" : "" %>>COMPLETED</option>
                        <option value="CANCELLED" <%= "CANCELLED".equals(selectedStatus) ? "selected" : "" %>>CANCELLED</option>
                        <option value="NO_SHOW" <%= "NO_SHOW".equals(selectedStatus) ? "selected" : "" %>>NO_SHOW</option>
                    </select>
                </div>
                <div class="col-md-2 d-grid">
                    <button class="btn btn-primary" type="submit">Filter</button>
                </div>
            </form>

            <% if (appointmentViews.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-0">No appointments match this filter.</p>
                </div>
            <% } else { %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle application-table">
                        <thead>
                        <tr>
                            <th>Appointment Number</th>
                            <th>Application Number</th>
                            <th>Applicant</th>
                            <th>Email</th>
                            <th>Office</th>
                            <th>Date</th>
                            <th>Start</th>
                            <th>End</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminAppointmentView item : appointmentViews) { %>
                            <tr>
                                <td><strong><%= value(item.getAppointment().getAppointmentNumber()) %></strong></td>
                                <td><%= value(item.getApplication().getApplicationNumber()) %></td>
                                <td><%= value(item.getApplication().getFullName()) %></td>
                                <td><%= value(item.getApplication().getEmail()) %></td>
                                <td><%= value(item.getOffice().getOfficeName()) %></td>
                                <td><%= value(item.getSlot().getAppointmentDate()) %></td>
                                <td><%= value(item.getSlot().getStartTime()) %></td>
                                <td><%= value(item.getSlot().getEndTime()) %></td>
                                <td>
                                    <span class="badge <%= statusClass(item.getAppointment().getStatus()) %>">
                                        <%= value(item.getAppointment().getStatus()) %>
                                    </span>
                                </td>
                                <td>
                                    <a class="btn btn-sm btn-primary" href="admin-appointments?action=details&id=<%= item.getAppointment().getId() %>">View Details</a>
                                </td>
                            </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            <% } %>
        </div>
    </div>
</main>
</body>
</html>
