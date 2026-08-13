<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    private String value(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    Object registrationEmailSent = session == null ? null : session.getAttribute("registrationEmailSent");
    Object registeredEmail = session == null ? null : session.getAttribute("registeredEmail");

    if (session != null) {
        session.removeAttribute("registrationEmailSent");
        session.removeAttribute("registeredEmail");
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Registration Successful</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-card card shadow-lg border-0">
        <div class="card-body p-4 p-md-5 text-center">
            <h2 class="mb-3">Registration Successful!</h2>
            <p class="text-muted mb-4">Your account has been created. You can now log in.</p>
            <% if (Boolean.TRUE.equals(registrationEmailSent)) { %>
                <div class="alert alert-success text-start" role="alert">
                    A registration confirmation email has been sent to <strong><%= value(registeredEmail) %></strong>.
                </div>
            <% } else if (Boolean.FALSE.equals(registrationEmailSent)) { %>
                <div class="alert alert-warning text-start" role="alert">
                    Your account was created successfully, but the registration confirmation email could not be sent. You can still log in and use PABS.
                </div>
            <% } %>
            <a class="btn btn-primary w-100" href="login.jsp">Go to Login</a>
        </div>
    </div>
</div>
</body>
</html>
