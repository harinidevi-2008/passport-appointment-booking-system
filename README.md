@"
# Passport Appointment Booking System

## 📌 Project Overview

The **Passport Appointment Booking System (PABS)** is a web-based application designed to simplify the process of passport appointment management.

The system provides separate functionality for normal users and administrators through role-based authentication. Users can register, log in, and access their user dashboard, while administrators can access the administrator dashboard and manage system-related operations.

This project is being developed incrementally through multiple modules.

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