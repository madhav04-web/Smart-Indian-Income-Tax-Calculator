package com.smarttax.config;

import java.sql.*;

public final class DatabaseConnection {
    private DatabaseConnection() {}

    public static Connection open() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/smart_income_tax_db?serverTimezone=Asia/Kolkata&connectTimeout=3000&socketTimeout=5000");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            String message;
            if (e.getErrorCode() == 1045) {
                message = "MySQL rejected the configured username or password. Set DB_USER and DB_PASSWORD in PowerShell, then restart the app.";
            } else if (e.getErrorCode() == 1049) {
                message = "The smart_income_tax_db database does not exist. Run database\\schema.sql against your MySQL server first.";
            } else if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                message = "Cannot reach MySQL at localhost:3306. Start or install MySQL Server 8, confirm it is listening on port 3306, then run database\\schema.sql. The current connection URL is " + url;
            } else {
                message = "MySQL database operation failed: " + e.getMessage();
            }
            throw new SQLException(message, e.getSQLState(), e.getErrorCode(), e);
        }
    }
}
