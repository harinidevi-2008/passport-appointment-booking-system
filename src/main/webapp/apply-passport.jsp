<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pabs.model.PassportApplication" %>
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
    if (!"USER".equalsIgnoreCase(role)) {
        response.sendRedirect("admin-dashboard.jsp");
        return;
    }

    PassportApplication passportApplication =
            (PassportApplication) request.getAttribute("application");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Apply for Passport</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg user-navbar">
    <div class="container">
        <a class="navbar-brand" href="user-dashboard.jsp">Passport Appointment Booking System</a>
        <div class="user-nav-links">
            <a class="nav-link" href="user-dashboard.jsp">Dashboard</a>
            <a class="nav-link active" href="passport-application">Apply Passport</a>
            <a class="nav-link" href="my-applications">My Applications</a>
            <a class="nav-link" href="profile">My Profile</a>
            <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="application-shell">
            <div class="page-heading mb-4">
                <p class="portal-kicker mb-2">Passport Application</p>
                <h1>Apply for Passport</h1>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert">
                    <%= request.getAttribute("errorMessage") %>
                </div>
            <% } %>

            <form action="passport-application" method="post">
                <section class="card form-section-card">
                    <div class="card-body">
                        <h2>Passport Details</h2>
                        <div class="row g-3">
                        <div class="col-md-6">
                            <label for="applicationType" class="form-label">Application Type</label>
                            <select class="form-select" id="applicationType" name="applicationType" required>
                                <option value="">Select application type</option>
                                <option value="Fresh Passport" <%= passportApplication != null && "Fresh Passport".equals(passportApplication.getApplicationType()) ? "selected" : "" %>>Fresh Passport</option>
                                <option value="Reissue" <%= passportApplication != null && "Reissue".equals(passportApplication.getApplicationType()) ? "selected" : "" %>>Reissue</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="passportMode" class="form-label">Passport Mode</label>
                            <select class="form-select" id="passportMode" name="passportMode" required>
                                <option value="">Select passport mode</option>
                                <option value="Normal" <%= passportApplication != null && "Normal".equals(passportApplication.getPassportMode()) ? "selected" : "" %>>Normal</option>
                                <option value="Tatkal" <%= passportApplication != null && "Tatkal".equals(passportApplication.getPassportMode()) ? "selected" : "" %>>Tatkal</option>
                            </select>
                        </div>
                        </div>
                    </div>
                </section>

                <section class="card form-section-card">
                    <div class="card-body">
                        <h2>Personal Details</h2>
                        <div class="row g-3">
                        <div class="col-md-6">
                            <label for="fullName" class="form-label">Full Name</label>
                            <input type="text" class="form-control" id="fullName" name="fullName" value="<%= passportApplication == null ? "" : value(passportApplication.getFullName()) %>" maxlength="100" required>
                        </div>
                        <div class="col-md-6">
                            <label for="dateOfBirth" class="form-label">Date of Birth</label>
                            <input type="date" class="form-control" id="dateOfBirth" name="dateOfBirth" value="<%= passportApplication == null || passportApplication.getDateOfBirth() == null ? "" : passportApplication.getDateOfBirth() %>" required>
                        </div>
                        <div class="col-md-6">
                            <label for="gender" class="form-label">Gender</label>
                            <select class="form-select" id="gender" name="gender" required>
                                <option value="">Select gender</option>
                                <option value="Male" <%= passportApplication != null && "Male".equals(passportApplication.getGender()) ? "selected" : "" %>>Male</option>
                                <option value="Female" <%= passportApplication != null && "Female".equals(passportApplication.getGender()) ? "selected" : "" %>>Female</option>
                                <option value="Other" <%= passportApplication != null && "Other".equals(passportApplication.getGender()) ? "selected" : "" %>>Other</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="placeOfBirth" class="form-label">Place of Birth</label>
                            <input type="text" class="form-control" id="placeOfBirth" name="placeOfBirth" value="<%= passportApplication == null ? "" : value(passportApplication.getPlaceOfBirth()) %>" maxlength="100" required>
                        </div>
                        </div>
                    </div>
                </section>

                <section class="card form-section-card">
                    <div class="card-body">
                        <h2>Family Details</h2>
                        <div class="row g-3">
                        <div class="col-md-6">
                            <label for="fatherName" class="form-label">Father Name</label>
                            <input type="text" class="form-control" id="fatherName" name="fatherName" value="<%= passportApplication == null ? "" : value(passportApplication.getFatherName()) %>" maxlength="100" required>
                        </div>
                        <div class="col-md-6">
                            <label for="motherName" class="form-label">Mother Name</label>
                            <input type="text" class="form-control" id="motherName" name="motherName" value="<%= passportApplication == null ? "" : value(passportApplication.getMotherName()) %>" maxlength="100" required>
                        </div>
                        </div>
                    </div>
                </section>

                <section class="card form-section-card">
                    <div class="card-body">
                        <h2>Contact Details</h2>
                        <div class="row g-3">
                        <div class="col-md-6">
                            <label for="phone" class="form-label">Phone</label>
                            <input type="tel" class="form-control" id="phone" name="phone" value="<%= passportApplication == null ? "" : value(passportApplication.getPhone()) %>" maxlength="20" pattern="\+?[0-9]{10,15}" required>
                        </div>
                        <div class="col-md-6">
                            <label for="email" class="form-label">Email</label>
                            <input type="email" class="form-control" id="email" name="email" value="<%= session.getAttribute("email") %>" readonly required>
                        </div>
                        </div>
                    </div>
                </section>

                <section class="card form-section-card">
                    <div class="card-body">
                        <h2>Address Details</h2>
                        <div class="row g-3">
                        <div class="col-12">
                            <label for="address" class="form-label">Address</label>
                            <textarea class="form-control" id="address" name="address" rows="3" maxlength="255" required><%= passportApplication == null ? "" : value(passportApplication.getAddress()) %></textarea>
                        </div>
                        <div class="col-md-6">
                            <label for="city" class="form-label">City</label>
                            <input type="text" class="form-control" id="city" name="city" value="<%= passportApplication == null ? "" : value(passportApplication.getCity()) %>" maxlength="50" required>
                        </div>
                        <div class="col-md-6">
                            <label for="state" class="form-label">State</label>
                            <input type="text" class="form-control" id="state" name="state" value="<%= passportApplication == null ? "" : value(passportApplication.getState()) %>" maxlength="50" required>
                        </div>
                        <div class="col-md-4">
                            <label for="pincode" class="form-label">Pincode</label>
                            <input type="text" class="form-control" id="pincode" name="pincode" value="<%= passportApplication == null ? "" : value(passportApplication.getPincode()) %>" maxlength="10" pattern="[0-9]{5,10}" required>
                        </div>
                        </div>
                    </div>
                </section>

                <div class="form-actions">
                    <a class="btn btn-outline-secondary btn-lg" href="user-dashboard.jsp">Cancel</a>
                    <button type="submit" class="btn btn-primary btn-lg">Submit Passport Application</button>
                </div>
            </form>
        </div>
    </div>
</main>
</body>
</html>
