<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Login</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css?v=pabs-auth-refined-20260818">
</head>
<body class="pabs-auth-page pabs-login-page">
<div class="auth-page auth-split">
    <div class="auth-shell">
        <section class="auth-brand-panel" aria-label="Passport Appointment Booking System">
            <div class="auth-brand-content">
                <div class="auth-brand-mark">PABS</div>
                <p class="auth-system-name">Passport Appointment Booking System</p>
                <h1>Your passport, made simpler.</h1>
                <p>Apply, schedule, and track each step through one secure citizen portal.</p>
                <div class="auth-document-visual" aria-hidden="true">
                    <div class="auth-passport">
                        <span class="passport-stamp">PASSPORT</span>
                        <span class="passport-seal"></span>
                        <span class="passport-line"></span>
                        <span class="passport-line short"></span>
                    </div>
                    <div class="auth-ticket">
                        <span>Appointment</span>
                        <strong>Confirmed</strong>
                    </div>
                    <div class="auth-date-card">
                        <span class="auth-date-month">APR</span>
                        <strong>18</strong>
                        <span>Slot booked</span>
                    </div>
                    <div class="auth-stamp">SECURE PORTAL</div>
                </div>
                <p class="auth-brand-footer">Secure. Simple. Reliable.</p>
            </div>
        </section>

        <section class="auth-form-panel">
            <header class="auth-panel-header">
                <div>
                    <strong>PABS</strong>
                    <span>Passport Appointment Booking System</span>
                </div>
                <a href="register.jsp">New here? Register</a>
            </header>

            <div class="auth-form-wrap">
                <h2>Welcome Back</h2>
                <p class="auth-intro">Sign in to continue to your Passport Appointment account.</p>

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

                <div class="mb-2">
                    <label for="password" class="form-label">Password</label>
                    <input type="password" class="form-control" id="password" name="password" required>
                </div>

                <div class="auth-form-meta">
                    <a href="forgot-password.jsp">Forgot Password?</a>
                </div>

                <button type="submit" class="btn btn-primary w-100">Login</button>
            </form>

            <div class="auth-divider"><span>or</span></div>

            <button type="button" class="btn google-auth-button w-100" disabled
                    aria-describedby="google-auth-note">
                <span class="google-mark" aria-hidden="true">G</span>
                Continue with Google
            </button>
            <p id="google-auth-note" class="auth-oauth-note mb-0">
                Google sign-in requires OAuth configuration before it can be enabled.
            </p>

            <p class="auth-link-row mt-4 mb-0">
                Don't have an account?
                <a href="register.jsp">Register</a>
            </p>
        </div>

            <footer class="auth-panel-footer">
                <span>&copy; 2026 Passport Appointment Booking System</span>
                <span>Secure Service Portal</span>
            </footer>
        </section>
    </div>
</div>
</body>
</html>
