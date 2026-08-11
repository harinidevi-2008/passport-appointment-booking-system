<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Login</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-card card shadow-lg border-0">
        <div class="card-body p-4 p-md-5">
            <h2 class="text-center mb-4">Login</h2>

            <% if ("1".equals(request.getParameter("error"))) { %>
                <div class="alert alert-danger" role="alert">
                    Invalid email or password.
                </div>
            <% } %>
            <%
                String loginSuccessMessage = (String) session.getAttribute("loginSuccessMessage");
                if (loginSuccessMessage != null) {
                    session.removeAttribute("loginSuccessMessage");
            %>
                <div class="alert alert-success" role="alert">
                    <%= loginSuccessMessage %>
                </div>
            <% } %>

            <form action="login" method="post">
                <div class="mb-3">
                    <label for="email" class="form-label">Email</label>
                    <input type="email" class="form-control" id="email" name="email" required>
                </div>

                <div class="mb-4">
                    <label for="password" class="form-label">Password</label>
                    <input type="password" class="form-control" id="password" name="password" required>
                </div>

                <button type="submit" class="btn btn-primary w-100">Login</button>
            </form>

            <p class="text-center mt-3 mb-0">
                <a href="forgot-password.jsp">Forgot Password?</a>
            </p>

            <p class="text-center mt-4 mb-0">
                Don't have an account?
                <a href="register.jsp">Register</a>
            </p>
        </div>
    </div>
</div>
</body>
</html>
