<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.pabs.model.Document" %>
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

    if (request.getAttribute("documentMaxSizeMb") == null || request.getAttribute("photoMaxSizeMb") == null) {
        response.sendRedirect("documents");
        return;
    }

    Document document = (Document) request.getAttribute("document");
    Object documentMaxSizeMb = request.getAttribute("documentMaxSizeMb");
    Object photoMaxSizeMb = request.getAttribute("photoMaxSizeMb");

    boolean hasIdentityProof = document != null && document.getIdentityProof() != null && !document.getIdentityProof().trim().isEmpty();
    boolean hasAddressProof = document != null && document.getAddressProof() != null && !document.getAddressProof().trim().isEmpty();
    boolean hasPhotograph = document != null && document.getPhotograph() != null && !document.getPhotograph().trim().isEmpty();
    boolean allUploaded = hasIdentityProof && hasAddressProof && hasPhotograph;
    String updatedAt = document != null && document.getUpdatedAt() != null ? document.getUpdatedAt().toString() : "Not available";
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Upload Documents</title>
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
            <a class="nav-link active" href="documents">Upload Documents</a>
            <a class="nav-link" href="appointment?action=my">My Appointments</a>
            <a class="btn btn-outline-light btn-sm" href="logout">Logout</a>
        </div>
    </div>
</nav>

<main class="dashboard-page user-portal-page">
    <div class="container">
        <div class="application-shell">
            <div class="page-heading mb-4">
                <p class="portal-kicker mb-2">Documents</p>
                <h1>Upload Documents</h1>
                <p class="mb-0">Upload identity proof, address proof, and a passport-size photograph.</p>
            </div>

            <% if ("1".equals(request.getParameter("uploaded"))) { %>
                <div class="alert alert-success" role="alert">
                    Documents uploaded successfully.
                    <% if (allUploaded) { %>
                        All required documents have been uploaded.
                    <% } %>
                </div>
            <% } %>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert">
                    <%= request.getAttribute("errorMessage") %>
                </div>
            <% } %>

            <section class="card form-section-card">
                <div class="card-body">
                    <h2>Current Document Status</h2>
                    <div class="row detail-row">
                        <div class="col-md-5 detail-label">Identity Proof</div>
                        <div class="col-md-7 detail-value">
                            <span class="badge <%= hasIdentityProof ? "text-bg-success" : "text-bg-secondary" %>"><%= hasIdentityProof ? "Uploaded" : "Not Uploaded" %></span>
                        </div>
                    </div>
                    <div class="row detail-row">
                        <div class="col-md-5 detail-label">Address Proof</div>
                        <div class="col-md-7 detail-value">
                            <span class="badge <%= hasAddressProof ? "text-bg-success" : "text-bg-secondary" %>"><%= hasAddressProof ? "Uploaded" : "Not Uploaded" %></span>
                        </div>
                    </div>
                    <div class="row detail-row">
                        <div class="col-md-5 detail-label">Passport Photograph</div>
                        <div class="col-md-7 detail-value">
                            <span class="badge <%= hasPhotograph ? "text-bg-success" : "text-bg-secondary" %>"><%= hasPhotograph ? "Uploaded" : "Not Uploaded" %></span>
                        </div>
                    </div>
                    <div class="row detail-row">
                        <div class="col-md-5 detail-label">Last Updated</div>
                        <div class="col-md-7 detail-value"><%= updatedAt %></div>
                    </div>
                    <% if (allUploaded) { %>
                        <p class="text-success fw-semibold mb-0 mt-3">All required documents have been uploaded.</p>
                    <% } %>
                </div>
            </section>

            <form action="documents" method="post" enctype="multipart/form-data">
                <section class="card form-section-card">
                    <div class="card-body">
                        <h2><%= allUploaded ? "Replace Documents" : "Upload Required Documents" %></h2>
                        <p class="text-muted">Identity and address proofs support PDF, JPG, JPEG, and PNG up to <%= documentMaxSizeMb %> MB each. Passport photograph supports JPG, JPEG, and PNG up to <%= photoMaxSizeMb %> MB.</p>

                        <div class="mb-3">
                            <label for="identityProof" class="form-label">Identity Proof</label>
                            <input type="file" class="form-control" id="identityProof" name="identityProof" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" <%= hasIdentityProof ? "" : "required" %>>
                            <% if (hasIdentityProof) { %>
                                <div class="form-text">Leave blank to keep the currently uploaded identity proof.</div>
                            <% } %>
                        </div>

                        <div class="mb-3">
                            <label for="addressProof" class="form-label">Address Proof</label>
                            <input type="file" class="form-control" id="addressProof" name="addressProof" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" <%= hasAddressProof ? "" : "required" %>>
                            <% if (hasAddressProof) { %>
                                <div class="form-text">Leave blank to keep the currently uploaded address proof.</div>
                            <% } %>
                        </div>

                        <div class="mb-4">
                            <label for="photograph" class="form-label">Passport Photograph</label>
                            <input type="file" class="form-control" id="photograph" name="photograph" accept=".jpg,.jpeg,.png,image/jpeg,image/png" <%= hasPhotograph ? "" : "required" %>>
                            <% if (hasPhotograph) { %>
                                <div class="form-text">Leave blank to keep the currently uploaded passport photograph.</div>
                            <% } %>
                        </div>

                        <div class="form-actions">
                            <a class="btn btn-outline-secondary" href="user-dashboard.jsp">Back to Dashboard</a>
                            <button type="submit" class="btn btn-primary">Upload Documents</button>
                        </div>
                    </div>
                </section>
            </form>
        </div>
    </div>
</main>
</body>
</html>
