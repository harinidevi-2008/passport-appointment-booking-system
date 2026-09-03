<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>User Registration</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css?v=pabs-auth-refined-20260818">
</head>
<body class="pabs-auth-page pabs-register-page">
<div class="auth-page auth-split">
    <div class="auth-shell">
        <section class="auth-brand-panel" aria-label="Passport Appointment Booking System">
            <div class="auth-brand-content">
                <div class="auth-brand-mark">PABS</div>
                <p class="auth-system-name">Passport Appointment Booking System</p>
                <h1>Manage your passport journey.</h1>
                <p>Create your account, submit applications, and manage appointments securely.</p>
                <div class="auth-document-visual" aria-hidden="true">
                    <div class="auth-passport">
                        <span class="passport-stamp">PASSPORT</span>
                        <span class="passport-seal"></span>
                        <span class="passport-line"></span>
                        <span class="passport-line short"></span>
                    </div>
                    <div class="auth-ticket">
                        <span>Application</span>
                        <strong>Ready</strong>
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
                <a href="login.jsp">Already have an account? Sign In</a>
            </header>

            <div class="auth-form-wrap">
                <h2>Create Your Account</h2>
                <p class="auth-intro">Register to manage your passport applications and appointments.</p>

            <form action="register" method="post">
                <div class="mb-3">
                    <label for="fullName" class="form-label">Full Name</label>
                    <input type="text" class="form-control" id="fullName" name="fullName" required>
                </div>

                <div class="mb-3">
                    <label for="email" class="form-label">Email</label>
                    <input type="email" class="form-control" id="email" name="email" required>
                </div>

                <div class="mb-4">
                    <label for="password" class="form-label">Password</label>
                    <input type="password" class="form-control" id="password" name="password" required>
                </div>

                <button type="submit" class="btn btn-primary w-100">Register</button>
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
                Already have an account?
                <a href="login.jsp">Sign In</a>
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
