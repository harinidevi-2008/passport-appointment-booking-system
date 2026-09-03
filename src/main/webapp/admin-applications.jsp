<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.pabs.controller.AdminApplicationServlet.AdminApplicationView" %>
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
        if ("APPROVED".equals(status) || "PROCESSING".equals(status)
                || "PRINTING".equals(status) || "DISPATCHED".equals(status) || "DELIVERED".equals(status)) {
            return "text-bg-success";
        }
        if ("REJECTED".equals(status)) {
            return "text-bg-danger";
        }
        if ("VERIFIED".equals(status)) {
            return "text-bg-success";
        }
        if ("UNDER_REVIEW".equals(status)) {
            return "text-bg-warning";
        }
        if ("SUBMITTED".equals(status)) {
            return "text-bg-info";
        }
        if ("CANCELLED".equals(status)) {
            return "text-bg-secondary";
        }
        return "text-bg-secondary";
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

    List<AdminApplicationView> applicationViews =
            (List<AdminApplicationView>) request.getAttribute("applicationViews");
    if (applicationViews == null) {
        response.sendRedirect("admin-applications");
        return;
    }
    String selectedStatus = (String) request.getAttribute("selectedStatus");
    String search = (String) request.getAttribute("search");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Application Processing</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark dashboard-nav">
    <div class="container">
        <a class="navbar-brand" href="admin-dashboard.jsp">Passport Appointment Booking System</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-light" href="admin-dashboard.jsp">Dashboard</a>
            <a class="btn btn-outline-light" href="admin-appointments">Appointment Management</a>
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
                    <h1 class="h3 mb-0">Application Processing</h1>
                </div>
                <a class="btn btn-outline-secondary align-self-md-start" href="admin-dashboard.jsp">Back to Dashboard</a>
            </div>

            <form action="admin-applications" method="get" class="row g-2 mb-4">
                <div class="col-md-3">
                    <select class="form-select" name="status">
                        <option value="ALL" <%= selectedStatus == null || selectedStatus.isEmpty() || "ALL".equals(selectedStatus) ? "selected" : "" %>>All Statuses</option>
                        <option value="SUBMITTED" <%= "SUBMITTED".equals(selectedStatus) ? "selected" : "" %>>SUBMITTED</option>
                        <option value="UNDER_REVIEW" <%= "UNDER_REVIEW".equals(selectedStatus) ? "selected" : "" %>>UNDER_REVIEW</option>
                        <option value="VERIFIED" <%= "VERIFIED".equals(selectedStatus) ? "selected" : "" %>>VERIFIED</option>
                        <option value="PROCESSING" <%= "PROCESSING".equals(selectedStatus) ? "selected" : "" %>>PROCESSING</option>
                        <option value="APPROVED" <%= "APPROVED".equals(selectedStatus) ? "selected" : "" %>>APPROVED</option>
                        <option value="PRINTING" <%= "PRINTING".equals(selectedStatus) ? "selected" : "" %>>PRINTING</option>
                        <option value="DISPATCHED" <%= "DISPATCHED".equals(selectedStatus) ? "selected" : "" %>>DISPATCHED</option>
                        <option value="DELIVERED" <%= "DELIVERED".equals(selectedStatus) ? "selected" : "" %>>DELIVERED</option>
                        <option value="REJECTED" <%= "REJECTED".equals(selectedStatus) ? "selected" : "" %>>REJECTED</option>
                        <option value="CANCELLED" <%= "CANCELLED".equals(selectedStatus) ? "selected" : "" %>>CANCELLED</option>
                    </select>
                </div>
                <div class="col-md-7">
                    <input class="form-control" type="search" name="search"
                           value="<%= value(search) %>"
                           placeholder="Search application number, applicant, or email">
                </div>
                <div class="col-md-2 d-grid">
                    <button class="btn btn-primary" type="submit">Filter</button>
                </div>
            </form>

            <% if (applicationViews.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-0">No passport applications are available.</p>
                </div>
            <% } else { %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle application-table">
                        <thead>
                        <tr>
                            <th>Application Number</th>
                            <th>Applicant</th>
                            <th>Email</th>
                            <th>Type</th>
                            <th>Mode</th>
                            <th>Submitted</th>
                            <th>Application Status</th>
                            <th>Appointment Status</th>
                            <th>Documents</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminApplicationView item : applicationViews) { %>
                            <tr>
                                <td><strong><%= value(item.getApplication().getApplicationNumber()) %></strong></td>
                                <td><%= value(item.getApplication().getFullName()) %></td>
                                <td><%= value(item.getApplication().getEmail()) %></td>
                                <td><%= value(item.getApplication().getApplicationType()) %></td>
                                <td><%= value(item.getApplication().getPassportMode()) %></td>
                                <td><%= value(item.getApplication().getCreatedAt()) %></td>
                                <td>
                                    <span class="badge <%= statusClass(item.getApplication().getStatus()) %>">
                                        <%= value(item.getApplication().getStatus()) %>
                                    </span>
                                    <% if ("CANCELLED".equals(item.getApplication().getStatus())
                                            && item.getApplication().getReviewNote() != null
                                            && !item.getApplication().getReviewNote().trim().isEmpty()) { %>
                                        <div class="small text-muted mt-1"><%= value(item.getApplication().getReviewNote()) %></div>
                                    <% } %>
                                </td>
                                <td><%= value(item.getAppointmentStatus()) %></td>
                                <td>
                                    <span class="badge <%= item.hasAllRequiredDocuments() ? "text-bg-success" : "text-bg-secondary" %>">
                                        <%= value(item.getDocumentStatus()) %>
                                    </span>
                                </td>
                                <td>
                                    <a class="btn btn-sm btn-primary" href="admin-applications?action=details&id=<%= item.getApplication().getId() %>">Review</a>
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
