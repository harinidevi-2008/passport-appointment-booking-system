<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Verify OTP</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-card card shadow-lg border-0">
        <div class="card-body p-4 p-md-5">
            <h2 class="text-center mb-3">Verify OTP</h2>
            <p class="text-center text-muted mb-4">
                Enter the 6-digit OTP sent to your registered email address.
            </p>

            <%
                String passwordResetMessage = (String) session.getAttribute("passwordResetMessage");
                if (passwordResetMessage != null) {
                    session.removeAttribute("passwordResetMessage");
            %>
                <div class="alert alert-info" role="alert">
                    <%= passwordResetMessage %>
                </div>
            <% } %>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-danger" role="alert">
                    <%= request.getAttribute("errorMessage") %>
                </div>
            <% } %>

            <form action="verify-otp" method="post" class="needs-validation" novalidate>
                <div class="mb-4">
                    <label for="otp" class="form-label">OTP</label>
                    <input type="text" class="form-control" id="otp" name="otp"
                           pattern="[0-9]{6}" maxlength="6" inputmode="numeric" required>
                    <div class="invalid-feedback">Please enter the 6-digit OTP.</div>
                </div>

                <button type="submit" class="btn btn-primary w-100">Verify OTP</button>
            </form>

            <p class="text-center mt-4 mb-0">
                <a href="forgot-password.jsp">Request a new OTP</a>
                <span class="text-muted mx-2">|</span>
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
