<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.pabs.model.Document" %>
<%!
    private String value(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
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

    if (request.getAttribute("documents") == null) {
        response.sendRedirect("admin-documents");
        return;
    }

    List<Document> documents = (List<Document>) request.getAttribute("documents");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Uploaded Documents</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark dashboard-nav">
    <div class="container">
        <a class="navbar-brand" href="admin-dashboard.jsp">Passport Appointment Booking System</a>
        <div class="d-flex gap-2 flex-wrap">
            <a class="btn btn-outline-light" href="admin-dashboard.jsp">Dashboard</a>
            <a class="btn btn-outline-light" href="admin-applications">Applications</a>
            <a class="btn btn-outline-light" href="admin-appointments">Appointments</a>
            <a class="btn btn-outline-light" href="admin-slots">Slot Management</a>
            <a class="btn btn-outline-light" href="admin-reports">Reports</a>
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
                    <h1 class="h3 mb-0">Uploaded Documents</h1>
                </div>
                <a class="btn btn-outline-secondary align-self-md-start" href="admin-dashboard.jsp">Back to Dashboard</a>
            </div>

            <% if (documents == null || documents.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-0">No citizens are available.</p>
                </div>
            <% } else { %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle application-table">
                        <thead>
                        <tr>
                            <th>Citizen</th>
                            <th>Email</th>
                            <th>Identity Proof</th>
                            <th>Address Proof</th>
                            <th>Photograph</th>
                            <th>Last Updated</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (Document document : documents) { %>
                            <%
                                boolean hasIdentityProof = document.getIdentityProof() != null && !document.getIdentityProof().trim().isEmpty();
                                boolean hasAddressProof = document.getAddressProof() != null && !document.getAddressProof().trim().isEmpty();
                                boolean hasPhotograph = document.getPhotograph() != null && !document.getPhotograph().trim().isEmpty();
                                String updatedAt = document.getUpdatedAt() != null ? document.getUpdatedAt().toString() : "Not uploaded";
                            %>
                            <tr>
                                <td><%= value(document.getUserFullName()) %></td>
                                <td><%= value(document.getUserEmail()) %></td>
                                <td><span class="badge <%= hasIdentityProof ? "text-bg-success" : "text-bg-secondary" %>"><%= hasIdentityProof ? "Uploaded" : "Missing" %></span></td>
                                <td><span class="badge <%= hasAddressProof ? "text-bg-success" : "text-bg-secondary" %>"><%= hasAddressProof ? "Uploaded" : "Missing" %></span></td>
                                <td><span class="badge <%= hasPhotograph ? "text-bg-success" : "text-bg-secondary" %>"><%= hasPhotograph ? "Uploaded" : "Missing" %></span></td>
                                <td><%= value(updatedAt) %></td>
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
