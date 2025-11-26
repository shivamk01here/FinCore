package com.fincore.handler;

import com.fincore.dao.NotificationDAO;
import com.fincore.dao.TransactionDAO;
import com.fincore.dao.UserDAO;
import com.fincore.model.User;
import com.fincore.server.HttpRequest;
import com.fincore.server.HttpResponse;
import java.math.BigDecimal;

public class TransactionHandler {
    private UserDAO userDAO = new UserDAO();
    private TransactionDAO transactionDAO = new TransactionDAO();
    private NotificationDAO notificationDAO = new NotificationDAO();

    public void handle(HttpRequest request, HttpResponse response) {
        response.setContentType("application/json");
        
        if (request.getPath().equals("/api/transaction/send") && request.getMethod().equals("POST")) {
            String body = request.getBody();
            int senderId = Integer.parseInt(extractJsonValue(body, "senderId"));
            String receiverVpa = extractJsonValue(body, "receiverVpa");
            BigDecimal amount = new BigDecimal(extractJsonValue(body, "amount"));

            User receiver = userDAO.getUserByVPA(receiverVpa);
            if (receiver == null) {
                response.setStatus(400, "Bad Request");
                response.setBody("{\"error\": \"Receiver not found\"}");
                return;
            }

            if (transactionDAO.sendMoney(senderId, receiver.getId(), amount)) {
                notificationDAO.addNotification(senderId, "Sent " + amount + " to " + receiverVpa);
                notificationDAO.addNotification(receiver.getId(), "Received " + amount + " from user " + senderId);
                response.setBody("{\"status\": \"success\"}");
            } else {
                response.setStatus(400, "Bad Request");
                response.setBody("{\"error\": \"Transaction failed (Insufficient balance)\"}");
            }
        }
    }

    private String extractJsonValue(String json, String key) {
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
