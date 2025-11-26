package com.fincore.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

public class SetupDB {
    public static void main(String[] args) {
        String schemaFile = "schema.sql";
        try (Connection conn = DBConnection.getConnection();
             BufferedReader reader = new BufferedReader(new FileReader(schemaFile));
             Statement stmt = conn.createStatement()) {

            System.out.println("Connected to Database. Executing schema.sql...");

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) continue;
                
                sql.append(line).append("\n");
                if (line.endsWith(";")) {
                    String query = sql.toString().replace(";", "");
                    System.out.println("EXECUTING QUERY: [" + query + "]");
                    try {
                        stmt.execute(query);
                        System.out.println("SUCCESS");
                    } catch (Exception e) {
                        System.out.println("ERROR: " + e.getMessage());
                        e.printStackTrace(System.out);
                    }
                    sql.setLength(0);
                } else {
                    sql.append(" ");
                }
            }
            System.out.println("Database Setup Complete.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
