package com.pabs.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private DBConnection() {
    }

    private static final String URL = "jdbc:mysql://localhost:3306/passport_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = System.getenv("PABS_DB_PASSWORD");

    public static Connection getConnection() {

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            return DriverManager.getConnection(
                    URL,
                    USERNAME,
                    PASSWORD
            );

        } catch (ClassNotFoundException | SQLException e) {

            e.printStackTrace();
            return null;

        }
    }
}
