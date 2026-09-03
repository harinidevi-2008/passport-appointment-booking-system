<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.Set" %>
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

    AdminApplicationView applicationView =
            (AdminApplicationView) request.getAttribute("applicationView");
    if (applicationView == null) {
        response.sendRedirect("admin-applications");
        return;
    }

    Set<String> nextStatuses = (Set<String>) request.getAttribute("nextStatuses");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    String dob = applicationView.getApplication().getDateOfBirth() == null
            ? ""
            : applicationView.getApplication().getDateOfBirth().format(dateFormatter);
    String submittedOn = applicationView.getApplication().getCreatedAt() == null
            ? ""
            : applicationView.getApplication().getCreatedAt().toLocalDateTime().format(dateFormatter);
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Review Application</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark dashboard-nav">
    <div class="container">
        <a class="navbar-brand" href="admin-dashboard.jsp">Passport Appointment Booking System</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-light" href="admin-applications">Application Processing</a>
            <a class="btn btn-outline-light" href="admin-appointments">Appointment Management</a>
            <a class="btn btn-outline-light" href="admin-documents">Document Status</a>
            <a class="btn btn-outline-light" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page">
    <div class="container">
        <div class="details-shell">
            <div class="details-header">
                <div>
                    <p class="text-uppercase text-muted fw-semibold mb-2">Application Review</p>
                    <h1><%= value(applicationView.getApplication().getApplicationNumber()) %></h1>
                    <p class="mb-0"><%= value(applicationView.getApplication().getFullName()) %></p>
                </div>
                <span class="badge <%= statusClass(applicationView.getApplication().getStatus()) %> status-badge">
                    <%= value(applicationView.getApplication().getStatus()) %>
                </span>
            </div>

            <% if ("1".equals(request.getParameter("updated"))) { %>
                <div class="alert alert-success" role="alert">Application status updated successfully.</div>
            <% } %>

            <% if ("1".equals(request.getParameter("mailError"))) { %>
                <div class="alert alert-warning" role="alert">
                    Application status was updated, but the email notification could not be sent.
                </div>
            <% } %>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert"><%= value(request.getAttribute("errorMessage")) %></div>
            <% } %>

            <div class="details-section">
                <h2 class="h5 mb-3">Application</h2>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Application Number</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getApplicationNumber()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Type</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getApplicationType()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Mode</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getPassportMode()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Submitted On</div>
                    <div class="col-md-8 detail-value"><%= value(submittedOn) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Application Status</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getStatus()) %></div>
                </div>
                <% if ("CANCELLED".equals(applicationView.getApplication().getStatus())
                        && applicationView.getApplication().getReviewNote() != null
                        && !applicationView.getApplication().getReviewNote().trim().isEmpty()) { %>
                    <div class="row detail-row">
                        <div class="col-md-4 detail-label">Reason / Context</div>
                        <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getReviewNote()) %></div>
                    </div>
                <% } %>
            </div>

            <div class="details-section">
                <h2 class="h5 mb-3">Latest Appointment</h2>
                <% if (applicationView.getAppointment() == null) { %>
                    <p class="mb-0 text-muted">No appointment has been booked for this application.</p>
                <% } else { %>
                    <div class="row detail-row">
                        <div class="col-md-4 detail-label">Appointment Number</div>
                        <div class="col-md-8 detail-value"><%= value(applicationView.getAppointment().getAppointmentNumber()) %></div>
                    </div>
                    <div class="row detail-row">
                        <div class="col-md-4 detail-label">Appointment Status</div>
                        <div class="col-md-8 detail-value"><%= value(applicationView.getAppointment().getStatus()) %></div>
                    </div>
                    <% if (applicationView.getSlot() != null) { %>
                        <div class="row detail-row">
                            <div class="col-md-4 detail-label">Schedule</div>
                            <div class="col-md-8 detail-value">
                                <%= value(applicationView.getSlot().getAppointmentDate()) %>
                                <%= value(applicationView.getSlot().getStartTime()) %>
                                - <%= value(applicationView.getSlot().getEndTime()) %>
                            </div>
                        </div>
                    <% } %>
                <% } %>
            </div>

            <div class="details-section">
                <h2 class="h5 mb-3">Applicant</h2>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Full Name</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getFullName()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Date of Birth</div>
                    <div class="col-md-8 detail-value"><%= value(dob) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Gender</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getGender()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Place of Birth</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getPlaceOfBirth()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Father</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getFatherName()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Mother</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getMotherName()) %></div>
                </div>
            </div>

            <div class="details-section">
                <h2 class="h5 mb-3">Contact</h2>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Phone</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getPhone()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Email</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getEmail()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Address</div>
                    <div class="col-md-8 detail-value"><%= value(applicationView.getApplication().getAddress()) %></div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">City / State / Pincode</div>
                    <div class="col-md-8 detail-value">
                        <%= value(applicationView.getApplication().getCity()) %>,
                        <%= value(applicationView.getApplication().getState()) %>
                        - <%= value(applicationView.getApplication().getPincode()) %>
                    </div>
                </div>
            </div>

            <div class="details-section">
                <h2 class="h5 mb-3">Documents</h2>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Identity Proof</div>
                    <div class="col-md-8 detail-value">
                        <span class="badge <%= applicationView.hasIdentityProof() ? "text-bg-success" : "text-bg-secondary" %>">
                            <%= applicationView.hasIdentityProof() ? "Uploaded" : "Missing" %>
                        </span>
                        <% if (applicationView.hasIdentityProof()) { %>
                            <a class="btn btn-sm btn-outline-primary ms-2" target="_blank"
                               href="admin-applications?action=document&id=<%= applicationView.getApplication().getId() %>&type=identity">View</a>
                        <% } %>
                    </div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Identity Validation</div>
                    <div class="col-md-8 detail-value">Pending Review</div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Address Proof</div>
                    <div class="col-md-8 detail-value">
                        <span class="badge <%= applicationView.hasAddressProof() ? "text-bg-success" : "text-bg-secondary" %>">
                            <%= applicationView.hasAddressProof() ? "Uploaded" : "Missing" %>
                        </span>
                        <% if (applicationView.hasAddressProof()) { %>
                            <a class="btn btn-sm btn-outline-primary ms-2" target="_blank"
                               href="admin-applications?action=document&id=<%= applicationView.getApplication().getId() %>&type=address">View</a>
                        <% } %>
                    </div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Address Validation</div>
                    <div class="col-md-8 detail-value">Pending Review</div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Passport Photograph</div>
                    <div class="col-md-8 detail-value">
                        <span class="badge <%= applicationView.hasPhotograph() ? "text-bg-success" : "text-bg-secondary" %>">
                            <%= applicationView.hasPhotograph() ? "Uploaded" : "Missing" %>
                        </span>
                        <% if (applicationView.hasPhotograph()) { %>
                            <a class="btn btn-sm btn-outline-primary ms-2" target="_blank"
                               href="admin-applications?action=document&id=<%= applicationView.getApplication().getId() %>&type=photograph">View</a>
                        <% } %>
                    </div>
                </div>
                <div class="row detail-row">
                    <div class="col-md-4 detail-label">Photograph Validation</div>
                    <div class="col-md-8 detail-value">Pending Review</div>
                </div>
            </div>

            <% if ("REJECTED".equals(applicationView.getApplication().getStatus())
                    && applicationView.getApplication().getReviewNote() != null
                    && !applicationView.getApplication().getReviewNote().trim().isEmpty()) { %>
                <div class="details-section">
                    <h2 class="h5 mb-3">Rejection Reason</h2>
                    <p class="mb-0"><%= value(applicationView.getApplication().getReviewNote()) %></p>
                </div>
            <% } %>

            <div class="details-section mb-0">
                <h2 class="h5 mb-3">Status Management</h2>
                <div class="status-guidance mb-3">
                    <strong>VERIFIED</strong> means initial checks passed and the citizen may book an appointment.
                    <strong>PROCESSING</strong> means a required appointment has been completed and final review can continue.
                    <strong>REJECTED</strong> means this application cannot be used for appointment booking.
                    <strong>APPROVED</strong> is the final positive application decision after required processing.
                </div>
                <% if ("VERIFIED".equals(applicationView.getApplication().getStatus())) { %>
                    <p class="mb-3 text-muted">Approval available after appointment completion.</p>
                <% } %>
                <% if (nextStatuses == null || nextStatuses.isEmpty()) { %>
                    <p class="mb-0 text-muted">No further status transitions are available for this application.</p>
                <% } else { %>
                    <form action="admin-applications?action=updateStatus" method="post" class="needs-validation" novalidate>
                        <input type="hidden" name="applicationId" value="<%= applicationView.getApplication().getId() %>">
                        <div class="mb-3">
                            <label class="form-label" for="status">Next Status</label>
                            <select class="form-select" id="status" name="status" required>
                                <option value="">Select status</option>
                                <% for (String status : nextStatuses) { %>
                                    <option value="<%= value(status) %>"><%= value(status) %></option>
                                <% } %>
                            </select>
                            <div class="invalid-feedback">Please select the next status.</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="reviewNote">Review Note / Rejection Reason</label>
                            <textarea class="form-control" id="reviewNote" name="reviewNote" rows="3"><%= value(request.getParameter("reviewNote")) %></textarea>
                        </div>
                        <div class="form-actions">
                            <a class="btn btn-outline-secondary" href="admin-applications">Back to Queue</a>
                            <button type="submit" class="btn btn-primary">Update Status</button>
                        </div>
                    </form>
                <% } %>
            </div>
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
