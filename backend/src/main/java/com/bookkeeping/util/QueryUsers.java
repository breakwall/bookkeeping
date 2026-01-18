package com.bookkeeping.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class QueryUsers {
    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = "./data/bookkeeping.db";
            String url = "jdbc:sqlite:" + dbPath;
            Connection conn = DriverManager.getConnection(url);
            
            System.out.println("=== User Information in Database ===\n");
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, username, email, password_hash, created_at FROM users ORDER BY id");
            
            boolean hasUsers = false;
            while (rs.next()) {
                hasUsers = true;
                System.out.println("User ID: " + rs.getLong("id"));
                System.out.println("Username: " + rs.getString("username"));
                String email = rs.getString("email");
                System.out.println("Email: " + (email != null && !email.isEmpty() ? email : "Not set"));
                System.out.println("Password Hash: " + rs.getString("password_hash"));
                System.out.println("Created At: " + rs.getString("created_at"));
                System.out.println("----------------------------------------");
            }
            
            if (!hasUsers) {
                System.out.println("No users found in database");
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
