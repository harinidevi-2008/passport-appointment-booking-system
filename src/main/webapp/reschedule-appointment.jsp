<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="com.pabs.controller.AppointmentServlet.AppointmentView" %>
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

    AppointmentView appointmentView = (AppointmentView) request.getAttribute("appointmentView");
    if (appointmentView == null) {
        response.sendRedirect("appointment?action=my");
        return;
    }

    List<PassportOffice> offices = (List<PassportOffice>) request.getAttribute("offices");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Reschedule Appointment</title>
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
                <h1>Reschedule Appointment</h1>
                <p class="mb-0">Choose a new passport office and future appointment date.</p>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert">
                    <%= request.getAttribute("errorMessage") %>
                </div>
            <% } %>

            <div class="details-section">
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Appointment Number</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getAppointment().getAppointmentNumber() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Current Schedule</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getSlot().getAppointmentDate() %> <%= appointmentView.getSlot().getStartTime() %> - <%= appointmentView.getSlot().getEndTime() %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Current Office</div>
                    <div class="col-md-8 detail-value"><%= appointmentView.getOffice().getOfficeName() %>, <%= appointmentView.getOffice().getCity() %></div>
                </div>
            </div>

            <% if (offices == null || offices.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-3">No active passport offices are available for appointment rescheduling.</p>
                    <a class="btn btn-outline-secondary" href="appointment?action=my">Back to My Appointments</a>
                </div>
            <% } else { %>
                <form action="appointment" method="get" class="needs-validation" novalidate>
                    <input type="hidden" name="action" value="rescheduleSlots">
                    <input type="hidden" name="appointmentId" value="<%= appointmentView.getAppointment().getId() %>">

                    <div class="card form-section-card">
                        <div class="card-body">
                            <h2>New Appointment Details</h2>

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
                                <a class="btn btn-outline-secondary" href="appointment?action=my">Cancel</a>
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
