<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="com.pabs.controller.AdminSlotManagementServlet.SlotManagementView" %>
<%@ page import="com.pabs.model.PassportOffice" %>
<%@ page import="com.pabs.service.AppointmentRecommendationService.OfficeDayMetrics" %>
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
    if (!"ADMIN".equalsIgnoreCase(role)) {
        response.sendRedirect("user-dashboard.jsp");
        return;
    }
    List<PassportOffice> offices = (List<PassportOffice>) request.getAttribute("offices");
    PassportOffice selectedOffice = (PassportOffice) request.getAttribute("selectedOffice");
    Integer selectedOfficeId = (Integer) request.getAttribute("selectedOfficeId");
    LocalDate selectedDate = (LocalDate) request.getAttribute("selectedDate");
    List<SlotManagementView> slotViews = (List<SlotManagementView>) request.getAttribute("slotViews");
    OfficeDayMetrics officeDayMetrics = (OfficeDayMetrics) request.getAttribute("officeDayMetrics");
    String actionStatus = request.getParameter("status");
    String actionMessage = request.getParameter("message");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Slot Management</title>
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
            <a class="btn btn-light" href="admin-slots">Slot Management</a>
            <a class="btn btn-outline-light" href="admin-reports">Reports</a>
            <a class="btn btn-outline-light" href="logout">Logout</a>
        </div>
    </div>
</nav>
<main class="dashboard-page">
    <div class="container">
        <div class="dashboard-panel mb-4">
            <p class="portal-kicker mb-2">Admin</p>
            <h1 class="h3 mb-0">Appointment Slot Management</h1>
        </div>

        <% if ("success".equals(actionStatus)) { %>
            <div class="alert alert-success"><%= value(actionMessage) %></div>
        <% } else if ("error".equals(actionStatus)) { %>
            <div class="alert alert-danger"><%= value(actionMessage) %></div>
        <% } %>

        <div class="dashboard-panel mb-4">
            <form action="admin-slots" method="get" class="row g-3 align-items-end">
                <div class="col-md-7">
                    <label class="form-label" for="officeId">Passport Office</label>
                    <select class="form-select" id="officeId" name="officeId" required>
                        <option value="">Select office</option>
                        <% for (PassportOffice office : offices) { %>
                            <option value="<%= office.getId() %>" <%= selectedOfficeId != null && selectedOfficeId == office.getId() ? "selected" : "" %>>
                                <%= value(office.getOfficeName()) %> - <%= value(office.getCity()) %>, <%= value(office.getState()) %>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label" for="date">Date</label>
                    <input type="date" class="form-control" id="date" name="date" value="<%= value(selectedDate) %>" required>
                </div>
                <div class="col-md-2">
                    <button type="submit" class="btn btn-primary w-100">View Slots</button>
                </div>
            </form>
        </div>

        <div class="dashboard-panel table-panel">
            <% if (selectedOfficeId == null) { %>
                <div class="empty-state">Select an office and date to view generated slots.</div>
            <% } else { %>
                <% if (selectedOffice != null && selectedDate != null) { %>
                    <div class="details-section">
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Office</div>
                            <div class="col-md-9 detail-value"><%= value(selectedOffice.getOfficeName()) %>, <%= value(selectedOffice.getCity()) %>, <%= value(selectedOffice.getState()) %></div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Appointment Date</div>
                            <div class="col-md-9 detail-value"><%= value(selectedDate) %></div>
                        </div>
                    </div>
                <% } %>
                <% if (officeDayMetrics != null) { %>
                    <section class="details-section info-section">
                        <h2>Office Crowd</h2>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Office Utilization</div>
                            <div class="col-md-9 detail-value"><%= officeDayMetrics.getUtilizationPercent() %>%</div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Daily Capacity</div>
                            <div class="col-md-9 detail-value"><%= officeDayMetrics.getDailyCapacity() %></div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Occupied</div>
                            <div class="col-md-9 detail-value"><%= officeDayMetrics.getDailyOccupied() %></div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Remaining Daily Capacity</div>
                            <div class="col-md-9 detail-value"><%= Math.max(officeDayMetrics.getDailyCapacity() - officeDayMetrics.getDailyOccupied(), 0) %></div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Crowd Level</div>
                            <div class="col-md-9 detail-value">
                                <span class="badge crowd-<%= value(officeDayMetrics.getCrowdLevel()).toLowerCase() %>"><%= value(officeDayMetrics.getCrowdLevel()) %></span>
                            </div>
                        </div>
                    </section>
                <% } %>
                <section class="slot-management-actions">
                    <div>
                        <h2 class="h5 mb-1">Slot Management</h2>
                        <p class="text-muted mb-0">Add a one-off slot or adjust capacity without deleting appointment history.</p>
                    </div>
                    <details class="slot-editor">
                        <summary class="btn btn-outline-primary">Add New Slot</summary>
                        <form action="admin-slots" method="post" class="slot-editor-form">
                            <input type="hidden" name="csrfToken" value="<%= CsrfUtil.getToken(session) %>">
                            <input type="hidden" name="action" value="createSlot">
                            <input type="hidden" name="officeId" value="<%= selectedOfficeId %>">
                            <input type="hidden" name="date" value="<%= value(selectedDate) %>">
                            <div>
                                <label class="form-label" for="newStartTime">Start Time</label>
                                <input type="time" class="form-control" id="newStartTime" name="startTime" required>
                            </div>
                            <div>
                                <label class="form-label" for="newEndTime">End Time</label>
                                <input type="time" class="form-control" id="newEndTime" name="endTime" required>
                            </div>
                            <div>
                                <label class="form-label" for="newCapacity">Slot Capacity</label>
                                <input type="number" class="form-control" id="newCapacity" name="capacity" min="1" value="3" required>
                            </div>
                            <button type="submit" class="btn btn-primary">Save Slot</button>
                        </form>
                    </details>
                </section>
                <% if (slotViews == null || slotViews.isEmpty()) { %>
                    <div class="empty-state">No slots are available for this office and date. Use Add New Slot to create one.</div>
                <% } else { %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle application-table">
                        <thead>
                        <tr>
                            <th>Date</th>
                            <th>Start</th>
                            <th>End</th>
                            <th>Capacity</th>
                            <th>Booked</th>
                            <th>Remaining</th>
                            <th>Slot Availability</th>
                            <th>State</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (SlotManagementView item : slotViews) { %>
                            <tr>
                                <td><%= value(item.getSlot().getAppointmentDate()) %></td>
                                <td><strong><%= value(item.getSlot().getStartTime()) %></strong></td>
                                <td><%= value(item.getSlot().getEndTime()) %></td>
                                <td>
                                    <div class="capacity-display">
                                        <span class="capacity-label">Capacity</span>
                                        <strong class="capacity-value"><%= item.getSlot().getCapacity() %></strong>
                                        <details class="capacity-editor">
                                            <summary>Edit</summary>
                                            <form action="admin-slots" method="post" class="capacity-editor-form">
                                                <input type="hidden" name="csrfToken" value="<%= CsrfUtil.getToken(session) %>">
                                                <input type="hidden" name="slotId" value="<%= item.getSlot().getId() %>">
                                                <input type="hidden" name="officeId" value="<%= selectedOfficeId %>">
                                                <input type="hidden" name="date" value="<%= value(selectedDate) %>">
                                                <input type="hidden" name="action" value="updateCapacity">
                                                <span>Booked: <%= item.getBookedCount() %></span>
                                                <label class="visually-hidden" for="capacity-<%= item.getSlot().getId() %>">New capacity</label>
                                                <input type="number" class="form-control form-control-sm" id="capacity-<%= item.getSlot().getId() %>"
                                                       name="capacity" min="<%= Math.max(item.getBookedCount(), 1) %>"
                                                       value="<%= item.getSlot().getCapacity() %>" required>
                                                <button type="submit" class="btn btn-sm btn-primary">Save</button>
                                            </form>
                                        </details>
                                    </div>
                                </td>
                                <td><%= item.getBookedCount() %></td>
                                <td><%= item.getRemainingCapacity() %></td>
                                <td><%= item.getBookedCount() %>/<%= item.getSlot().getCapacity() %> booked</td>
                                <td><span class="badge <%= item.getSlot().isActive() ? "text-bg-success" : "text-bg-secondary" %>"><%= item.getSlot().isActive() ? "ACTIVE" : "INACTIVE" %></span></td>
                                <td>
                                    <form action="admin-slots" method="post" class="m-0">
                                        <input type="hidden" name="csrfToken" value="<%= CsrfUtil.getToken(session) %>">
                                        <input type="hidden" name="slotId" value="<%= item.getSlot().getId() %>">
                                        <input type="hidden" name="officeId" value="<%= selectedOfficeId %>">
                                        <input type="hidden" name="date" value="<%= value(selectedDate) %>">
                                        <input type="hidden" name="action" value="<%= item.getSlot().isActive() ? "deactivate" : "activate" %>">
                                        <button type="submit" class="btn btn-sm <%= item.getSlot().isActive() ? "btn-outline-danger" : "btn-outline-success" %>">
                                            <%= item.getSlot().isActive() ? "Deactivate" : "Activate" %>
                                        </button>
                                    </form>
                                </td>
                            </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
            <% } %>
        </div>
    </div>
</main>
</body>
</html>
