<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    if (session == null || session.getAttribute("userId") == null) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>User Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark dashboard-nav">
    <div class="container">
        <span class="navbar-brand">Passport Appointment Booking System</span>
        <a class="btn btn-outline-light" href="logout">Logout</a>
    </div>
</nav>

<main class="dashboard-page">
    <div class="container">
        <div class="dashboard-panel">
            <p class="text-uppercase text-muted fw-semibold mb-2">User Dashboard</p>
            <h1>Welcome, <%= session.getAttribute("fullName") %></h1>
            <p class="lead mb-0">Passport Appointment Booking System</p>
        </div>
    </div>
</main>
</body>
</html>
