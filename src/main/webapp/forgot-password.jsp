<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Forgot Password</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-card card shadow-lg border-0">
        <div class="card-body p-4 p-md-5">
            <h2 class="text-center mb-3">Forgot Password</h2>
            <p class="text-center text-muted mb-4">
                Enter your registered email address to receive a password reset OTP.
            </p>

            <% if ("1".equals(request.getParameter("expired"))) { %>
                <div class="alert alert-warning" role="alert">
                    Your OTP has expired. Please request a new OTP.
                </div>
            <% } %>

            <% if ("1".equals(request.getParameter("attempts"))) { %>
                <div class="alert alert-warning" role="alert">
                    Too many incorrect attempts. Please request a new OTP.
                </div>
            <% } %>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert">
                    <%= request.getAttribute("errorMessage") %>
                </div>
            <% } %>

            <form action="forgot-password" method="post" class="needs-validation" novalidate>
                <div class="mb-4">
                    <label for="email" class="form-label">Email</label>
                    <input type="email" class="form-control" id="email" name="email" required>
                    <div class="invalid-feedback">Please enter a valid email address.</div>
                </div>

                <button type="submit" class="btn btn-primary w-100">Send OTP</button>
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
