package com.fincore.dao;

import com.fincore.model.Transaction;
import com.fincore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public boolean sendMoney(int senderId, int receiverId, BigDecimal amount) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Deduct from sender
            String deductSql = "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?";
            try (PreparedStatement deductStmt = conn.prepareStatement(deductSql)) {
                deductStmt.setBigDecimal(1, amount);
                deductStmt.setInt(2, senderId);
                deductStmt.setBigDecimal(3, amount);
                int rowsAffected = deductStmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    return false; // Insufficient balance or user not found
                }
            }

            // 2. Add to receiver
            String addSql = "UPDATE users SET balance = balance + ? WHERE id = ?";
            try (PreparedStatement addStmt = conn.prepareStatement(addSql)) {
                addStmt.setBigDecimal(1, amount);
                addStmt.setInt(2, receiverId);
                addStmt.executeUpdate();
            }

            // 3. Record transaction
            String recordSql = "INSERT INTO transactions (sender_id, receiver_id, amount) VALUES (?, ?, ?)";
            try (PreparedStatement recordStmt = conn.prepareStatement(recordSql)) {
                recordStmt.setInt(1, senderId);
                recordStmt.setInt(2, receiverId);
                recordStmt.setBigDecimal(3, amount);
                recordStmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Transaction> getTransactionsByUserId(int userId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE sender_id = ? OR receiver_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                transactions.add(new Transaction(
                    rs.getInt("id"),
                    rs.getInt("sender_id"),
                    rs.getInt("receiver_id"),
                    rs.getBigDecimal("amount"),
                    rs.getTimestamp("timestamp")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }
}
