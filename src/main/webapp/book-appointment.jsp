<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="com.pabs.controller.AppointmentServlet" %>
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

    List<PassportApplication> eligibleApplications =
            (List<PassportApplication>) request.getAttribute("eligibleApplications");
    List<PassportApplication> applications =
            (List<PassportApplication>) request.getAttribute("applications");
    List<PassportOffice> offices = (List<PassportOffice>) request.getAttribute("offices");
    Integer selectedApplicationId = (Integer) request.getAttribute("selectedApplicationId");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Book Appointment</title>
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
            <a class="nav-link" href="profile">My Profile</a>
            <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="application-shell">
            <div class="page-heading mb-4">
                <p class="portal-kicker mb-2">Appointments</p>
                <h1>Book Appointment</h1>
                <p class="mb-0">Select your application, passport office, and preferred appointment date.</p>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert">
                    <%= request.getAttribute("errorMessage") %>
                </div>
            <% } %>

            <% if (applications != null && !applications.isEmpty()) { %>
                <div class="application-eligibility-list mb-4">
                    <% for (PassportApplication item : applications) {
                        boolean canBook = AppointmentServlet.isEligibleForAppointmentBooking(item);
                    %>
                        <div class="eligibility-row <%= canBook ? "is-eligible" : "is-blocked" %>">
                            <div>
                                <strong><%= item.getApplicationNumber() %></strong>
                                <span><%= item.getApplicationType() %> - <%= item.getStatus() %></span>
                            </div>
                            <p class="mb-0"><%= canBook ? "Verified: this application is eligible for appointment booking." : AppointmentServlet.appointmentEligibilityMessage(item) %></p>
                        </div>
                    <% } %>
                </div>
            <% } %>

            <% if (eligibleApplications == null || eligibleApplications.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-3">No VERIFIED passport applications without an active appointment are available for appointment booking.</p>
                    <a class="btn btn-primary" href="my-applications">View My Applications</a>
                </div>
            <% } else if (offices == null || offices.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-3">No active passport offices are available for appointment booking.</p>
                    <a class="btn btn-outline-secondary" href="user-dashboard.jsp">Back to Dashboard</a>
                </div>
            <% } else { %>
                <form action="appointment" method="get" class="needs-validation" novalidate>
                    <input type="hidden" name="action" value="slots">

                    <div class="card form-section-card">
                        <div class="card-body">
                            <h2>Appointment Details</h2>

                            <div class="mb-3">
                                <label for="applicationId" class="form-label">Passport Application</label>
                                <select class="form-select" id="applicationId" name="applicationId" required>
                                    <option value="">Select application</option>
                                    <% for (PassportApplication passportApplication : eligibleApplications) { %>
                                        <option value="<%= passportApplication.getId() %>" <%= selectedApplicationId != null && selectedApplicationId == passportApplication.getId() ? "selected" : "" %>>
                                            <%= passportApplication.getApplicationNumber() %> - <%= passportApplication.getApplicationType() %> (<%= passportApplication.getStatus() %>)
                                        </option>
                                    <% } %>
                                </select>
                                <div class="invalid-feedback">Please select a passport application.</div>
                            </div>

                            <div class="mb-3">
                                <label for="officeId" class="form-label">Passport Office / PSK / POPSK</label>
                                <select class="form-select" id="officeId" name="officeId" required>
                                    <option value="">Select office</option>
                                    <% for (PassportOffice office : offices) { %>
                                        <option value="<%= office.getId() %>">
                                            <%= office.getOfficeName() %> - <%= office.getCity() %>, <%= office.getState() %>
                                        </option>
                                    <% } %>
                                </select>
                                <div class="invalid-feedback">Please select an active passport office.</div>
                            </div>

                            <div class="mb-4">
                                <label for="date" class="form-label">Appointment Date</label>
                                <input type="date" class="form-control" id="date" name="date"
                                       min="<%= LocalDate.now() %>" required>
                                <div class="invalid-feedback">Please select a valid appointment date.</div>
                            </div>

                            <div class="form-actions">
                                <a class="btn btn-outline-secondary" href="user-dashboard.jsp">Cancel</a>
                                <button type="submit" class="btn btn-primary">Check Available Slots</button>
                            </div>
                        </div>
                    </div>
                </form>
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
