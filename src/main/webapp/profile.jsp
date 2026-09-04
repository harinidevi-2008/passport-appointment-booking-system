<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pabs.model.User" %>
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

    User profileUser = (User) request.getAttribute("user");
    if (profileUser == null) {
        response.sendRedirect("profile");
        return;
    }

    boolean editMode = Boolean.TRUE.equals(request.getAttribute("editMode"));
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My Profile</title>
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
            <a class="nav-link" href="documents">Upload Documents</a>
            <a class="nav-link" href="appointment?action=my">My Appointments</a>
            <a class="nav-link active" href="profile">My Profile</a>
            <a class="nav-link" href="change-password">Change Password</a>
            <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="details-shell profile-shell">
            <div class="details-header">
                <div>
                    <p class="portal-kicker mb-2">My Profile</p>
                    <h1>Account Information</h1>
                    <p class="mb-0">Manage your current PABS account details.</p>
                </div>
                <% if (!editMode) { %>
                    <div class="d-flex gap-2 flex-wrap">
                        <a class="btn btn-primary" href="profile?mode=edit">Edit Profile</a>
                        <a class="btn btn-outline-primary" href="change-password">Change Password</a>
                    </div>
                <% } %>
            </div>

            <% if ("1".equals(request.getParameter("updated"))) { %>
                <div class="alert alert-success" role="alert">Your profile was updated successfully.</div>
            <% } %>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert"><%= value(request.getAttribute("errorMessage")) %></div>
            <% } %>

            <% if (editMode) { %>
                <form action="profile" method="post" class="needs-validation" novalidate>
                    <input type="hidden" name="csrfToken" value="<%= CsrfUtil.getToken(session) %>">
                    <section class="card form-section-card">
                        <div class="card-body">
                            <h2>Personal Information</h2>
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label for="fullName" class="form-label">Full Name</label>
                                    <input type="text" class="form-control" id="fullName" name="fullName"
                                           value="<%= value(profileUser.getFullName()) %>" maxlength="100" required>
                                    <div class="invalid-feedback">Please enter your full name.</div>
                                </div>
                                <div class="col-md-6">
                                    <label for="email" class="form-label">Email</label>
                                    <input type="email" class="form-control" id="email" name="email"
                                           value="<%= value(profileUser.getEmail()) %>" maxlength="100" required>
                                    <div class="invalid-feedback">Please enter a valid email address.</div>
                                </div>
                            </div>
                        </div>
                    </section>
                    <div class="form-actions">
                        <a class="btn btn-outline-secondary" href="profile">Cancel</a>
                        <button type="submit" class="btn btn-primary">Save Changes</button>
                    </div>
                </form>
            <% } else { %>
                <div class="profile-summary">
                    <div class="profile-row">
                        <span>Full Name</span>
                        <strong><%= value(profileUser.getFullName()) %></strong>
                    </div>
                    <div class="profile-row">
                        <span>Email</span>
                        <strong><%= value(profileUser.getEmail()) %></strong>
                    </div>
                    <div class="profile-row">
                        <span>Account Role</span>
                        <strong><%= value(profileUser.getRole()) %></strong>
                    </div>
                    <div class="profile-row">
                        <span>Registered On</span>
                        <strong><%= value(profileUser.getCreatedAt()) %></strong>
                    </div>
                </div>
            <% } %>
        </div>
    </div>
</main>
<script>
    (() => {
        const forms = document.querySelectorAll('.needs-validation');
        Array.from(forms).forEach(form => {
            form.addEventListener('submit', event => {
                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                form.classList.add('was-validated');
            }, false);
        });
    })();
</script>
</body>
</html>
