<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="com.pabs.controller.AppointmentServlet" %>
<%@ page import="com.pabs.controller.AppointmentServlet.AppointmentSlotView" %>
<%@ page import="com.pabs.controller.AppointmentServlet.AppointmentView" %>
<%@ page import="com.pabs.model.PassportApplication" %>
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
    if (!"USER".equalsIgnoreCase(role)) {
        response.sendRedirect("admin-dashboard.jsp");
        return;
    }
    PassportApplication passportApplication = (PassportApplication) request.getAttribute("application");
    
    PassportOffice office = (PassportOffice) request.getAttribute("office");
    LocalDate appointmentDate = (LocalDate) request.getAttribute("appointmentDate");
    List<AppointmentSlotView> slotViews = (List<AppointmentSlotView>) request.getAttribute("slotViews");
    OfficeDayMetrics officeDayMetrics = (OfficeDayMetrics) request.getAttribute("officeDayMetrics");
    Boolean isReschedule = (Boolean) request.getAttribute("isReschedule");
    AppointmentView appointmentView = (AppointmentView) request.getAttribute("appointmentView");
    boolean rescheduleMode = Boolean.TRUE.equals(isReschedule) && appointmentView != null;
    AppointmentSlotView recommendedSlotView = null;
    if (slotViews != null) {
        for (AppointmentSlotView slotView : slotViews) {
            if (slotView.isRecommended()) {
                recommendedSlotView = slotView;
                break;
            }
        }
    }
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
            <a class="nav-link active" href="appointment?action=book">Smart Recommendations</a>
            <a class="nav-link" href="appointment?action=my">My Appointments</a>
            <a class="nav-link" href="profile">My Profile</a>
            <a class="nav-link" href="change-password">Change Password</a>
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
                    <h1 class="h3 mb-0"><%= rescheduleMode ? "Available Reschedule Slots" : "Available Slots" %></h1>
                </div>
                <a class="btn btn-outline-secondary align-self-md-start" href="<%= rescheduleMode ? "appointment?action=reschedule&appointmentId=" + appointmentView.getAppointment().getId() : "appointment?action=book" %>">Change Search</a>
            </div>

            <div class="details-section">
                <% if (rescheduleMode) { %>
                    <div class="row detail-row">
                        <div class="col-md-3 detail-label">Appointment</div>
                        <div class="col-md-9 detail-value"><%= appointmentView.getAppointment().getAppointmentNumber() %></div>
                    </div>
                <% } %>
                <div class="row detail-row">
                    <div class="col-md-3 detail-label">Application</div>
                    <div class="col-md-9 detail-value"><%= passportApplication.getApplicationNumber() %> (<%= passportApplication.getStatus() %>)</div>
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
                        <div class="col-md-3 detail-label">Available</div>
                        <div class="col-md-9 detail-value"><%= Math.max(officeDayMetrics.getDailyCapacity() - officeDayMetrics.getDailyOccupied(), 0) %></div>
                    </div>
                    <div class="row detail-row">
                        <div class="col-md-3 detail-label">Crowd Level</div>
                        <div class="col-md-9 detail-value">
                            <span class="badge crowd-<%= value(officeDayMetrics.getCrowdLevel()).toLowerCase() %>">
                                <%= value(officeDayMetrics.getCrowdLevel()) %>
                            </span>
                        </div>
                    </div>
                </section>
            <% } %>

            <% if (!rescheduleMode && !AppointmentServlet.isEligibleForAppointmentBooking(passportApplication)) { %>
                <div class="alert alert-warning" role="alert">
                    <%= AppointmentServlet.appointmentEligibilityMessage(passportApplication) %>
                </div>
            <% } else if (slotViews == null || slotViews.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-3">No available appointment slots were found for the selected office and date.</p>
                    <a class="btn btn-primary" href="<%= rescheduleMode ? "appointment?action=reschedule&appointmentId=" + appointmentView.getAppointment().getId() : "appointment?action=book" %>">Choose Another Date</a>
                </div>
            <% } else { %>
                <% if (recommendedSlotView != null) { %>
                    <section class="details-section info-section recommended-slot-section">
                        <h2>Smart Slot Recommendation</h2>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Recommendation</div>
                            <div class="col-md-9 detail-value">Recommended</div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Time</div>
                            <div class="col-md-9 detail-value">
                                <strong><%= recommendedSlotView.getSlot().getStartTime() %> - <%= recommendedSlotView.getSlot().getEndTime() %></strong>
                            </div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Capacity</div>
                            <div class="col-md-9 detail-value"><%= recommendedSlotView.getSlot().getCapacity() %></div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Booked</div>
                            <div class="col-md-9 detail-value"><%= recommendedSlotView.getBookedCount() %></div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Remaining</div>
                            <div class="col-md-9 detail-value"><%= recommendedSlotView.getRemainingCapacity() %></div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Crowd Level</div>
                            <div class="col-md-9 detail-value">
                                <span class="badge crowd-<%= value(recommendedSlotView.getCrowdLevel()).toLowerCase() %>">
                                    <%= value(recommendedSlotView.getCrowdLevel()) %>
                                </span>
                            </div>
                        </div>
                        <div class="row detail-row">
                            <div class="col-md-3 detail-label">Reason</div>
                            <div class="col-md-9 detail-value"><%= value(recommendedSlotView.getReason()) %></div>
                        </div>
                    </section>
                <% } %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle application-table">
                        <thead>
                        <tr>
                            <th>Start Time</th>
                            <th>End Time</th>
                            <th>Capacity</th>
                            <th>Booked</th>
                            <th>Remaining</th>
                            <th>Recommendation</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AppointmentSlotView slotView : slotViews) { %>
                            <tr class="<%= slotView.isRecommended() ? "recommended-table-row" : "" %>">
                                <td><strong><%= slotView.getSlot().getStartTime() %></strong></td>
                                <td><%= slotView.getSlot().getEndTime() %></td>
                                <td><%= slotView.getSlot().getCapacity() %></td>
                                <td><%= slotView.getBookedCount() %></td>
                                <td><%= slotView.getRemainingCapacity() %></td>
                                <td><%= slotView.isRecommended() ? "Recommended" : "Available" %></td>
                                <td>
                                    <form action="appointment?action=<%= rescheduleMode ? "rescheduleConfirm" : "confirm" %>" method="post" class="m-0">
                                        <input type="hidden" name="csrfToken" value="<%= CsrfUtil.getToken(session) %>">
                                        <% if (rescheduleMode) { %>
                                            <input type="hidden" name="appointmentId" value="<%= appointmentView.getAppointment().getId() %>">
                                        <% } else { %>
                                            <input type="hidden" name="applicationId" value="<%= passportApplication.getId() %>">
                                        <% } %>
                                        <input type="hidden" name="slotId" value="<%= slotView.getSlot().getId() %>">
                                        <button type="submit" class="btn btn-sm btn-primary"><%= rescheduleMode ? "Reschedule" : "Book Slot" %></button>
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
