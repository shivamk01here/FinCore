package com.fincore.handler;

import com.fincore.dao.NotificationDAO;
import com.fincore.dao.TransactionDAO;
import com.fincore.dao.UserDAO;
import com.fincore.model.Notification;
import com.fincore.model.Transaction;
import com.fincore.model.User;
import com.fincore.server.HttpRequest;
import com.fincore.server.HttpResponse;

import java.util.List;

public class DashboardHandler {
    private UserDAO userDAO = new UserDAO();
    private TransactionDAO transactionDAO = new TransactionDAO();
    private NotificationDAO notificationDAO = new NotificationDAO();

    public void handle(HttpRequest request, HttpResponse response) {
        response.setContentType("application/json");

        if (request.getPath().equals("/api/dashboard") && request.getMethod().equals("GET")) {
            handleGetDashboard(request, response);
        } else if (request.getPath().equals("/api/admin/pending") && request.getMethod().equals("GET")) {
            handleGetPendingUsers(request, response);
        } else if (request.getPath().equals("/api/admin/approve") && request.getMethod().equals("POST")) {
            handleApproveUser(request, response);
        } else {
            response.setStatus(404, "Not Found");
            response.setBody("{\"error\": \"Endpoint not found\"}");
        }
    }

    private void handleGetDashboard(HttpRequest request, HttpResponse response) {
        String userIdStr = request.getQueryParam("userId");
        if (userIdStr == null) {
            response.setStatus(400, "Bad Request");
            response.setBody("{\"error\": \"Missing userId\"}");
            return;
        }

        int userId = Integer.parseInt(userIdStr);
        User user = userDAO.getUserById(userId);
        List<Transaction> transactions = transactionDAO.getTransactionsByUserId(userId);
        List<Notification> notifications = notificationDAO.getNotificationsByUserId(userId);

        // Construct JSON manually
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"user\": {");
        json.append("\"username\": \"").append(user.getUsername()).append("\",");
        json.append("\"balance\": ").append(user.getBalance()).append(",");
        json.append("\"vpa\": \"").append(user.getVpa()).append("\"");
        json.append("},");
        
        json.append("\"transactions\": [");
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            json.append(String.format("{\"id\": %d, \"amount\": %s, \"senderId\": %d, \"receiverId\": %d}", 
                t.getId(), t.getAmount(), t.getSenderId(), t.getReceiverId()));
            if (i < transactions.size() - 1) json.append(",");
        }
        json.append("],");

        json.append("\"notifications\": [");
        for (int i = 0; i < notifications.size(); i++) {
            Notification n = notifications.get(i);
            json.append(String.format("{\"message\": \"%s\"}", n.getMessage()));
            if (i < notifications.size() - 1) json.append(",");
        }
        json.append("]");
        json.append("}");

        response.setBody(json.toString());
    }

    private void handleGetPendingUsers(HttpRequest request, HttpResponse response) {
        List<User> users = userDAO.getPendingUsers();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            json.append(String.format("{\"id\": %d, \"username\": \"%s\", \"fullName\": \"%s\", \"accountType\": \"%s\"}", 
                u.getId(), u.getUsername(), u.getFullName(), u.getAccountType()));
            if (i < users.size() - 1) json.append(",");
        }
        json.append("]");
        response.setBody(json.toString());
    }

    private void handleApproveUser(HttpRequest request, HttpResponse response) {
        String body = request.getBody();
        String userIdStr = extractJsonValue(body, "userId");
        if (userIdStr != null) {
            int userId = Integer.parseInt(userIdStr);
            if (userDAO.approveUser(userId)) {
                response.setBody("{\"status\": \"success\"}");
            } else {
                response.setStatus(500, "Error");
                response.setBody("{\"error\": \"Failed to approve\"}");
            }
        }
    }

    private String extractJsonValue(String json, String key) {
        // Simplified extraction
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        while (!Character.isDigit(json.charAt(start)) && json.charAt(start) != '"') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}')) end++;
        return json.substring(start, end).replace("\"", "").trim();
    }
}
