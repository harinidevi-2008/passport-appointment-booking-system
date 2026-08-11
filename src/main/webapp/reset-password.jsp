<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    if (session == null || !Boolean.TRUE.equals(session.getAttribute("passwordResetVerified"))) {
        response.sendRedirect("forgot-password.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Reset Password</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-card card shadow-lg border-0">
        <div class="card-body p-4 p-md-5">
            <h2 class="text-center mb-3">Reset Password</h2>
            <p class="text-center text-muted mb-4">
                Choose a new password for your account.
            </p>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert">
                    <%= request.getAttribute("errorMessage") %>
                </div>
            <% } %>

            <form action="reset-password" method="post" class="needs-validation" novalidate>
                <div class="mb-3">
                    <label for="password" class="form-label">New Password</label>
                    <input type="password" class="form-control" id="password" name="password" minlength="8" required>
                    <div class="invalid-feedback">Password must be at least 8 characters long.</div>
                </div>

                <div class="mb-4">
                    <label for="confirmPassword" class="form-label">Confirm New Password</label>
                    <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" minlength="8" required>
                    <div class="invalid-feedback">Please confirm your new password.</div>
                </div>

                <button type="submit" class="btn btn-primary w-100">Reset Password</button>
            </form>

            <p class="text-center mt-4 mb-0">
                <a href="login.jsp">Back to Login</a>
            </p>
        </div>
    </div>
</div>
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
