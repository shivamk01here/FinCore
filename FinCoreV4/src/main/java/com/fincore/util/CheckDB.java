package com.fincore.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDB {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

            System.out.println("--- Users ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + 
                                   ", User: " + rs.getString("username") + 
                                   ", Role: " + rs.getString("role") + 
                                   ", Status: " + rs.getString("status"));
            }
            System.out.println("-------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
