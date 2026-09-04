<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="com.pabs.controller.AppointmentServlet" %>
<%@ page import="com.pabs.model.PassportApplication" %>
<%@ page import="com.pabs.model.PassportOffice" %>
<%@ page import="com.pabs.service.AppointmentRecommendationService.OfficeRecommendation" %>
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
    List<OfficeRecommendation> officeRecommendations =
            (List<OfficeRecommendation>) request.getAttribute("officeRecommendations");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Book Appointment</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css?v=smart-office-cards-20260904">
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

            <% if (eligibleApplications != null && !eligibleApplications.isEmpty()) { %>
                <div class="dashboard-panel mb-4">
                    <form action="appointment" method="get" class="row g-3 align-items-end">
                        <input type="hidden" name="action" value="book">
                        <div class="col-lg-9">
                            <label for="recommendationApplicationId" class="form-label">Verified Application</label>
                            <select class="form-select" id="recommendationApplicationId" name="applicationId" required>
                                <option value="">Select a verified application to see recommended offices</option>
                                <% for (PassportApplication passportApplication : eligibleApplications) { %>
                                    <option value="<%= passportApplication.getId() %>" <%= selectedApplicationId != null && selectedApplicationId == passportApplication.getId() ? "selected" : "" %>>
                                        <%= passportApplication.getApplicationNumber() %> - <%= passportApplication.getApplicationType() %>
                                    </option>
                                <% } %>
                            </select>
                        </div>
                        <div class="col-lg-3">
                            <button type="submit" class="btn btn-primary w-100">Show Recommended Offices</button>
                        </div>
                    </form>
                </div>
            <% } %>

            <% if (officeRecommendations != null && !officeRecommendations.isEmpty()) { %>
                <div class="recommendation-panel mb-4">
                    <div class="d-flex flex-column flex-md-row justify-content-between gap-2 mb-3">
                        <div>
                            <h2 class="h5 mb-1">Smart Office Recommendation</h2>
                            <p class="text-muted mb-0">Based on your registered location, approximate distance, office/day utilization, appointment availability, and earliest appointment.</p>
                        </div>
                    </div>
                    <div class="smart-office-grid">
                        <% int officeLimit = Math.min(3, officeRecommendations.size());
                           for (int i = 0; i < officeLimit; i++) {
                               OfficeRecommendation recommendation = officeRecommendations.get(i);
                               PassportOffice recommendedOffice = recommendation.getOffice();
                        %>
                            <article class="recommendation-card <%= recommendation.isRecommended() ? "recommendation-card--recommended" : "" %>">
                                    <div class="smart-office-header">
                                        <span class="recommendation-mark"><%= recommendation.isRecommended() ? "Recommended" : "Alternative" %></span>
                                        <h3><%= recommendedOffice.getOfficeName() %></h3>
                                        <p><%= recommendedOffice.getCity() %>, <%= recommendedOffice.getState() %></p>
                                    </div>
                                    <div class="smart-office-metrics">
                                        <div class="smart-office-metric"><span class="smart-office-metric-label">Approx. Distance</span><strong class="smart-office-metric-value"><%= String.format("%.1f", recommendation.getDistanceKm()) %> km</strong></div>
                                        <div class="smart-office-metric"><span class="smart-office-metric-label">Office Crowd</span><strong class="smart-office-metric-value"><span class="badge crowd-<%= recommendation.getCrowdLevel().toLowerCase() %>"><%= recommendation.getCrowdLevel() %></span></strong></div>
                                        <div class="smart-office-metric"><span class="smart-office-metric-label">Daily Utilization</span><strong class="smart-office-metric-value"><%= recommendation.getAverageUtilizationPercent() %>%</strong></div>
                                        <div class="smart-office-metric"><span class="smart-office-metric-label">Daily Capacity</span><strong class="smart-office-metric-value"><%= recommendation.getDailyCapacity() %></strong></div>
                                        <div class="smart-office-metric"><span class="smart-office-metric-label">Occupied</span><strong class="smart-office-metric-value"><%= recommendation.getDailyOccupied() %></strong></div>
                                        <div class="smart-office-metric"><span class="smart-office-metric-label">Available Capacity</span><strong class="smart-office-metric-value"><%= recommendation.getAvailableSlots() %> seats</strong></div>
                                        <div class="smart-office-metric"><span class="smart-office-metric-label">Earliest Appointment</span><strong class="smart-office-metric-value"><%= recommendation.getEarliestDate() %> <%= recommendation.getEarliestStartTime() %></strong></div>
                                    </div>
                                    <div class="recommendation-reasons">
                                        <strong><%= recommendation.isRecommended() ? "Recommended because:" : "Why this office:" %></strong>
                                        <ul>
                                            <% if (recommendation.getDistanceKm() <= 75.0) { %><li>reasonable distance from your stored city/state</li><% } %>
                                            <% if ("LOW".equals(recommendation.getCrowdLevel())) { %><li>low office/day crowd</li><% } %>
                                            <% if (recommendation.getAvailableSlots() >= 6) { %><li>good available capacity</li><% } %>
                                            <% if (recommendation.getEarliestDate() != null) { %><li>early appointment availability</li><% } %>
                                        </ul>
                                    </div>
                                    <form action="appointment" method="get" class="mt-3">
                                        <input type="hidden" name="action" value="slots">
                                        <input type="hidden" name="applicationId" value="<%= selectedApplicationId %>">
                                        <input type="hidden" name="officeId" value="<%= recommendedOffice.getId() %>">
                                        <input type="hidden" name="date" value="<%= recommendation.getEarliestDate() %>">
                                        <button type="submit" class="btn btn-primary smart-office-cta">View Slots <span aria-hidden="true">-&gt;</span></button>
                                    </form>
                            </article>
                        <% } %>
                    </div>
                </div>
            <% } %>

            <% if (eligibleApplications == null || eligibleApplications.isEmpty()) { %>
                <div class="empty-state">
                    <p class="mb-3">No VERIFIED passport applications without an active appointment are available for appointment booking.</p>
                    <a class="btn btn-primary" href="my-applications">View My Applications</a>
                </div>
            <% } else if (selectedApplicationId == null) { %>
                <div class="empty-state">
                    <p class="mb-0">Select a verified application above to view smart office recommendations before choosing an office manually.</p>
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
