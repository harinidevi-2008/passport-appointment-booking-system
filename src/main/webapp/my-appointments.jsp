<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
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

    List<AppointmentView> appointmentViews =
            (List<AppointmentView>) request.getAttribute("appointmentViews");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My Appointments</title>
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
        <div class="dashboard-panel table-panel">
            <div class="d-flex flex-column flex-md-row justify-content-between gap-3 mb-4">
                <div>
                    <p class="portal-kicker mb-2">Appointments</p>
                    <h1 class="h3 mb-0">My Appointments</h1>
                </div>
                <a class="btn btn-primary align-self-md-start" href="appointment?action=book">Book Appointment</a>
            </div>

            <% if ("1".equals(request.getParameter("cancelled"))) { %>
                <div class="alert alert-success" role="alert">Appointment cancelled successfully.</div>
            <% } %>

            <% if ("1".equals(request.getParameter("cancelError"))) { %>
                <div class="alert alert-danger" role="alert">Unable to cancel the selected appointment.</div>
            <% } %>

            <% if ("1".equals(request.getParameter("rescheduleError"))) { %>
                <div class="alert alert-danger" role="alert">Unable to reschedule the selected appointment.</div>
            <% } %>

            <% if (appointmentViews == null || appointmentViews.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-3">You do not have any appointments yet.</p>
                    <a class="btn btn-primary" href="appointment?action=book">Book Appointment</a>
                </div>
            <% } else { %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle application-table">
                        <thead>
                        <tr>
                            <th>Appointment Number</th>
                            <th>Application Number</th>
                            <th>Office</th>
                            <th>Date</th>
                            <th>Time</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AppointmentView item : appointmentViews) { %>
                            <tr>
                                <td><strong><%= item.getAppointment().getAppointmentNumber() %></strong></td>
                                <td><%= item.getApplication().getApplicationNumber() %></td>
                                <td><%= item.getOffice().getOfficeName() %></td>
                                <td><%= item.getSlot().getAppointmentDate() %></td>
                                <td><%= item.getSlot().getStartTime() %> - <%= item.getSlot().getEndTime() %></td>
                                <td><span class="badge text-bg-primary"><%= item.getAppointment().getStatus() %></span></td>
                                <td>
                                    <div class="d-flex flex-wrap gap-2">
                                        <a class="btn btn-sm btn-primary" href="appointment?action=details&id=<%= item.getAppointment().getId() %>">View Details</a>
                                        <% if (item.isReschedulable()) { %>
                                            <a class="btn btn-sm btn-outline-primary" href="appointment?action=reschedule&appointmentId=<%= item.getAppointment().getId() %>">Reschedule</a>
                                        <% } %>
                                        <% if (item.isCancellable()) { %>
                                            <form action="appointment?action=cancel" method="post" class="m-0">
                                                <input type="hidden" name="appointmentId" value="<%= item.getAppointment().getId() %>">
                                                <button type="submit" class="btn btn-sm btn-outline-danger">Cancel</button>
                                            </form>
                                        <% } %>
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
