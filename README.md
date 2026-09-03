@"
# Passport Appointment Booking System

## 📌 Project Overview

The **Passport Appointment Booking System (PABS)** is a web-based application designed to simplify the process of passport appointment management.

The system provides separate functionality for normal users and administrators through role-based authentication. Users can register, log in, and access their user dashboard, while administrators can access the administrator dashboard and manage system-related operations.

This project is being developed incrementally through multiple modules.

---

## Email Configuration

PABS sends email for password reset OTPs, application updates, and appointment notifications. SMTP credentials must be configured with environment variables before starting Tomcat.

1. Create or configure the email sender account. For Gmail, use an app password rather than your normal account password.
2. Set these environment variables:

```text
PABS_SMTP_HOST=smtp.gmail.com
PABS_SMTP_PORT=587
PABS_SMTP_USERNAME=your-email@example.com
PABS_SMTP_PASSWORD=your-app-password
PABS_SMTP_FROM=your-email@example.com
```

`PABS_SMTP_USERNAME`, `PABS_SMTP_PASSWORD`, and `PABS_SMTP_FROM` are required for delivery. `PABS_SMTP_HOST` and `PABS_SMTP_PORT` default to Gmail SMTP values when omitted.

The older `PABS_MAIL_USERNAME`, `PABS_MAIL_PASSWORD`, `PABS_MAIL_HOST`, `PABS_MAIL_PORT`, and `PABS_MAIL_FROM` names are still accepted as fallbacks, but new setups should use `PABS_SMTP_*`.

3. Run `database/password_reset_otp.sql` once against `passport_db` to create the password reset OTP table.
4. Start the application.
5. From `login.jsp`, click `Forgot Password?`, enter the registered email address, and submit.
6. Enter the OTP received by email, then set and confirm the new password.

Never commit real mail passwords, SMTP secrets, API keys, or `.env` files.

---

## 🚀 Current Module

### Module 1 – User Authentication and Registration

**Status: Completed ✅**

The following functionality has been implemented:

- User registration
- User login
- Role-based authentication
- User dashboard
- Admin dashboard
- Session management
- Logout functionality
- Invalid login handling
- MySQL database connectivity
- MVC-based project structure
- Responsive web interface
- Landing page for the application

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 | Backend programming |
| Jakarta Servlets | Request handling |
| JSP | Dynamic web pages |
| HTML5 | Page structure |
| CSS3 | Styling |
| Bootstrap 5 | Responsive UI |
| JDBC | Database connectivity |
| MySQL | Database |
| Apache Tomcat 10.1 | Web server |
| Apache Maven | Project/build management |

---

## 🏗️ Project Architecture

The project follows an MVC-oriented structure.

```text
PABS/
│
├── pom.xml
├── README.md
├── .gitignore
│
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── pabs/
        │           ├── controller/
        │           │   ├── LoginServlet.java
        │           │   ├── RegisterServlet.java
        │           │   └── LogoutServlet.java
        │           │
        │           ├── dao/
        │           │   └── UserDAO.java
        │           │
        │           ├── model/
        │           │   └── User.java
        │           │
        │           └── util/
        │               └── DBConnection.java
        │
        └── webapp/
            ├── css/
            │   └── style.css
            │
            ├── WEB-INF/
            │   └── web.xml
            │
            ├── index.jsp
            ├── login.jsp
            ├── register.jsp
            ├── register-success.jsp
            ├── user-dashboard.jsp
            └── admin-dashboard.jsp
