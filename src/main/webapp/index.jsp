<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Passport Appointment Booking System</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="landing-page">
<nav class="navbar navbar-expand-lg landing-nav">
    <div class="container">
        <a class="navbar-brand d-flex align-items-center gap-2" href="index.jsp">
            <span class="brand-icon"><i class="bi bi-passport"></i></span>
            <span>Passport Appointment Booking System</span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#landingNavbar"
                aria-controls="landingNavbar" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="landingNavbar">
            <ul class="navbar-nav ms-auto align-items-lg-center">
                <li class="nav-item">
                    <a class="nav-link active" aria-current="page" href="index.jsp">Home</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="login.jsp">Login</a>
                </li>
                <li class="nav-item">
                    <a class="btn btn-warning nav-register-btn" href="register.jsp">Register</a>
                </li>
            </ul>
        </div>
    </div>
</nav>

<main>
    <section class="hero-section">
        <div class="container">
            <div class="row align-items-center g-5">
                <div class="col-lg-7">
                    <p class="section-kicker">Official passport service portal</p>
                    <h1>Passport Appointment Booking System</h1>
                    <p class="hero-subtitle">Book and manage your passport appointments with ease.</p>
                    <p class="hero-description">
                        A convenient portal for citizens to register, log in, and manage passport appointment
                        services through a simple and secure online process.
                    </p>
                    <div class="hero-actions d-flex flex-column flex-sm-row gap-3">
                        <a class="btn btn-warning btn-lg" href="login.jsp">
                            <i class="bi bi-box-arrow-in-right me-2"></i>Login
                        </a>
                        <a class="btn btn-outline-light btn-lg" href="register.jsp">
                            <i class="bi bi-person-plus me-2"></i>Register
                        </a>
                    </div>
                </div>
                <div class="col-lg-5">
                    <div class="passport-visual" aria-label="Passport appointment services illustration">
                        <div class="passport-card">
                            <div class="passport-emblem">
                                <i class="bi bi-passport"></i>
                            </div>
                            <div>
                                <p class="passport-label">Citizen Services</p>
                                <h2>Passport Appointment</h2>
                            </div>
                            <div class="document-lines">
                                <span></span>
                                <span></span>
                                <span></span>
                            </div>
                        </div>
                        <div class="appointment-tile">
                            <i class="bi bi-calendar-check"></i>
                            <div>
                                <strong>Appointment Ready</strong>
                                <span>Secure account access</span>
                            </div>
                        </div>
                        <div class="security-tile">
                            <i class="bi bi-shield-lock"></i>
                            <span>Authenticated portal</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <section class="content-section">
        <div class="container">
            <div class="section-heading text-center">
                <p class="section-kicker">Portal Benefits</p>
                <h2>Why Use Our Portal?</h2>
                <p>Designed to make passport appointment access clear, organized, and easy to use.</p>
            </div>
            <div class="row g-4">
                <div class="col-md-6 col-lg-3">
                    <div class="feature-card h-100">
                        <i class="bi bi-person-vcard"></i>
                        <h3>Easy Registration</h3>
                        <p>Create your account quickly and securely.</p>
                    </div>
                </div>
                <div class="col-md-6 col-lg-3">
                    <div class="feature-card h-100">
                        <i class="bi bi-lock"></i>
                        <h3>Secure Login</h3>
                        <p>Access your account through authenticated login.</p>
                    </div>
                </div>
                <div class="col-md-6 col-lg-3">
                    <div class="feature-card h-100">
                        <i class="bi bi-calendar2-check"></i>
                        <h3>Appointment Management</h3>
                        <p>Manage your passport appointment details conveniently.</p>
                    </div>
                </div>
                <div class="col-md-6 col-lg-3">
                    <div class="feature-card h-100">
                        <i class="bi bi-list-check"></i>
                        <h3>User-Friendly Process</h3>
                        <p>A simple and organized workflow for passport services.</p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <section class="workflow-section">
        <div class="container">
            <div class="section-heading text-center">
                <p class="section-kicker">Simple Workflow</p>
                <h2>How It Works</h2>
            </div>
            <div class="row g-4">
                <div class="col-md-4">
                    <div class="workflow-step">
                        <span>1</span>
                        <h3>Register</h3>
                        <p>Create your user account.</p>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="workflow-step">
                        <span>2</span>
                        <h3>Login</h3>
                        <p>Access the passport booking portal.</p>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="workflow-step">
                        <span>3</span>
                        <h3>Manage Appointments</h3>
                        <p>Manage your passport appointment-related activities.</p>
                    </div>
                </div>
            </div>
        </div>
    </section>
</main>

<footer class="landing-footer">
    <div class="container d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
        <div>
            <strong>Passport Appointment Booking System</strong>
            <p class="mb-0">Copyright &copy; 2026 Passport Appointment Booking System. All rights reserved.</p>
        </div>
        <div class="footer-links">
            <a href="index.jsp">Home</a>
            <a href="login.jsp">Login</a>
            <a href="register.jsp">Register</a>
        </div>
    </div>
</footer>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
