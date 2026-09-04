<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.pabs.dao.AdminReportDAO.DailyAppointmentReportRow" %>
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
    if (!"ADMIN".equalsIgnoreCase(role)) {
        response.sendRedirect("user-dashboard.jsp");
        return;
    }

    LocalDate selectedDate = (LocalDate) request.getAttribute("selectedDate");
    Map<String, Integer> applicationSummary = (Map<String, Integer>) request.getAttribute("applicationSummary");
    Map<String, Integer> appointmentSummary = (Map<String, Integer>) request.getAttribute("appointmentSummary");
    List<DailyAppointmentReportRow> dailyAppointments =
            (List<DailyAppointmentReportRow>) request.getAttribute("dailyAppointments");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Admin Reports</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark dashboard-nav">
    <div class="container">
        <span class="navbar-brand">Passport Appointment Booking System</span>
        <div class="d-flex gap-2 flex-wrap">
            <a class="btn btn-outline-light" href="admin-dashboard.jsp">Dashboard</a>
            <a class="btn btn-outline-light" href="admin-applications">Applications</a>
            <a class="btn btn-outline-light" href="admin-appointments">Appointments</a>
            <a class="btn btn-outline-light" href="admin-slots">Slot Management</a>
            <a class="btn btn-light" href="admin-reports">Reports</a>
            <a class="btn btn-outline-light" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page">
    <div class="container">
        <div class="dashboard-panel mb-4">
            <p class="portal-kicker mb-2">Administrative Reports</p>
            <h1 class="h3 mb-0">Reports</h1>
        </div>

        <div class="report-grid mb-4">
            <div class="report-card">
                <div class="report-card-label">Total Citizens</div>
                <div class="report-card-value"><%= value(request.getAttribute("totalCitizens")) %></div>
            </div>
            <div class="report-card">
                <div class="report-card-label">Total Applications</div>
                <div class="report-card-value"><%= value(request.getAttribute("totalApplications")) %></div>
            </div>
            <div class="report-card">
                <div class="report-card-label">Verified Applications</div>
                <div class="report-card-value"><%= value(request.getAttribute("verifiedApplications")) %></div>
            </div>
            <div class="report-card">
                <div class="report-card-label">Processing Applications</div>
                <div class="report-card-value"><%= value(request.getAttribute("processingApplications")) %></div>
            </div>
            <div class="report-card">
                <div class="report-card-label">Total Appointments</div>
                <div class="report-card-value"><%= value(request.getAttribute("totalAppointments")) %></div>
            </div>
            <div class="report-card">
                <div class="report-card-label">Completed Appointments</div>
                <div class="report-card-value"><%= value(request.getAttribute("completedAppointments")) %></div>
            </div>
            <div class="report-card">
                <div class="report-card-label">Cancelled Appointments</div>
                <div class="report-card-value"><%= value(request.getAttribute("cancelledAppointments")) %></div>
            </div>
            <div class="report-card">
                <div class="report-card-label">NO_SHOW Appointments</div>
                <div class="report-card-value"><%= value(request.getAttribute("noShowAppointments")) %></div>
            </div>
        </div>

        <div class="row g-4 mb-4">
            <div class="col-lg-6">
                <div class="dashboard-panel h-100">
                    <h2 class="h5 mb-3">Application Status Summary</h2>
                    <table class="table table-sm application-table status-summary-table">
                        <thead>
                        <tr>
                            <th>Status</th>
                            <th class="text-end">Count</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (Map.Entry<String, Integer> entry : applicationSummary.entrySet()) { %>
                            <tr><td><%= value(entry.getKey()) %></td><td class="text-end"><strong><%= entry.getValue() %></strong></td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="dashboard-panel h-100">
                    <h2 class="h5 mb-3">Appointment Status Summary</h2>
                    <table class="table table-sm application-table status-summary-table">
                        <thead>
                        <tr>
                            <th>Status</th>
                            <th class="text-end">Count</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (Map.Entry<String, Integer> entry : appointmentSummary.entrySet()) { %>
                            <tr><td><%= value(entry.getKey()) %></td><td class="text-end"><strong><%= entry.getValue() %></strong></td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div class="dashboard-panel table-panel">
            <div class="d-flex flex-column flex-md-row justify-content-between gap-3 mb-3">
                <div>
                    <h2 class="h5 mb-1">Daily Appointments</h2>
                    <p class="text-muted mb-0">Appointments scheduled for the selected date.</p>
                </div>
                <div class="d-flex flex-column flex-sm-row gap-2">
                    <form action="admin-reports" method="get" class="d-flex gap-2">
                        <input type="date" class="form-control" name="date" value="<%= value(selectedDate) %>" required>
                        <button type="submit" class="btn btn-primary">View</button>
                    </form>
                    <a class="btn btn-outline-primary" href="admin-reports/pdf?date=<%= value(selectedDate) %>">Download PDF Report</a>
                </div>
            </div>
            <% if (dailyAppointments == null || dailyAppointments.isEmpty()) { %>
                <div class="empty-state">No appointments found for this date.</div>
            <% } else { %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle application-table">
                        <thead>
                        <tr>
                            <th>Application</th>
                            <th>Citizen</th>
                            <th>Office</th>
                            <th>Date</th>
                            <th>Start</th>
                            <th>End</th>
                            <th>Status</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (DailyAppointmentReportRow row : dailyAppointments) { %>
                            <tr>
                                <td><%= value(row.getApplicationNumber()) %></td>
                                <td><%= value(row.getCitizenName()) %></td>
                                <td><%= value(row.getOfficeName()) %></td>
                                <td><%= value(row.getAppointmentDate()) %></td>
                                <td><%= value(row.getStartTime()) %></td>
                                <td><%= value(row.getEndTime()) %></td>
                                <td><span class="badge text-bg-secondary"><%= value(row.getAppointmentStatus()) %></span></td>
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
