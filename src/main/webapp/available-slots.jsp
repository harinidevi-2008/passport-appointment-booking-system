<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="com.pabs.controller.AppointmentServlet.AppointmentSlotView" %>
<%@ page import="com.pabs.model.PassportApplication" %>
<%@ page import="com.pabs.model.PassportOffice" %>
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
PassportApplication passportApplication = (PassportApplication) request.getAttribute("application");
    
    PassportOffice office = (PassportOffice) request.getAttribute("office");
    LocalDate appointmentDate = (LocalDate) request.getAttribute("appointmentDate");
    List<AppointmentSlotView> slotViews = (List<AppointmentSlotView>) request.getAttribute("slotViews");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Available Appointment Slots</title>
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
                    <h1 class="h3 mb-0">Available Slots</h1>
                </div>
                <a class="btn btn-outline-secondary align-self-md-start" href="appointment?action=book">Change Search</a>
            </div>

            <div class="details-section">
                <div class="row detail-row">
                    <div class="col-md-3 detail-label">Application</div>
                    <div class="col-md-9 detail-value"><%= passportApplication.getApplicationNumber() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-3 detail-label">Office</div>
                    <div class="col-md-9 detail-value"><%= office.getOfficeName() %>, <%= office.getCity() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-3 detail-label">Date</div>
                    <div class="col-md-9 detail-value"><%= appointmentDate %></div>
                </div>
            </div>

            <% if (slotViews == null || slotViews.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-3">No available appointment slots were found for the selected office and date.</p>
                    <a class="btn btn-primary" href="appointment?action=book">Choose Another Date</a>
                </div>
            <% } else { %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle application-table">
                        <thead>
                        <tr>
                            <th>Start Time</th>
                            <th>End Time</th>
                            <th>Remaining Capacity</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AppointmentSlotView slotView : slotViews) { %>
                            <tr>
                                <td><strong><%= slotView.getSlot().getStartTime() %></strong></td>
                                <td><%= slotView.getSlot().getEndTime() %></td>
                                <td><%= slotView.getRemainingCapacity() %></td>
                                <td>
                                    <form action="appointment?action=confirm" method="post" class="m-0">
                                        <input type="hidden" name="applicationId" value="<%= passportApplication.getId() %>">
                                        <input type="hidden" name="slotId" value="<%= slotView.getSlot().getId() %>">
                                        <button type="submit" class="btn btn-sm btn-primary">Book Slot</button>
                                    </form>
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
