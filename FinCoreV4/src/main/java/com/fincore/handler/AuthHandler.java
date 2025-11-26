package com.fincore.handler;

import com.fincore.dao.UserDAO;
import com.fincore.model.User;
import com.fincore.server.HttpRequest;
import com.fincore.server.HttpResponse;
import java.math.BigDecimal;

public class AuthHandler {
    private UserDAO userDAO = new UserDAO();

    public void handle(HttpRequest request, HttpResponse response) {
        response.setContentType("application/json");

        if (request.getPath().equals("/api/auth/login") && request.getMethod().equals("POST")) {
            handleLogin(request, response);
        } else if (request.getPath().equals("/api/auth/register") && request.getMethod().equals("POST")) {
            handleRegister(request, response);
        } else {
            response.setStatus(404, "Not Found");
            response.setBody("{\"error\": \"Endpoint not found\"}");
        }
    }

    private void handleLogin(HttpRequest request, HttpResponse response) {
        String body = request.getBody();
        // Simple JSON parsing (manual for now to avoid deps)
        String username = extractJsonValue(body, "username");
        String password = extractJsonValue(body, "password");

        User user = userDAO.login(username, password);
        if (user != null) {
            if ("ACTIVE".equals(user.getStatus()) || "ADMIN".equals(user.getRole())) {
                response.setBody(String.format("{\"status\": \"success\", \"role\": \"%s\", \"userId\": %d, \"username\": \"%s\"}", 
                    user.getRole(), user.getId(), user.getUsername()));
            } else {
                response.setStatus(403, "Forbidden");
                response.setBody("{\"error\": \"Account pending approval\"}");
            }
        } else {
            response.setStatus(401, "Unauthorized");
            response.setBody("{\"error\": \"Invalid credentials\"}");
        }
    }

    private void handleRegister(HttpRequest request, HttpResponse response) {
        String body = request.getBody();
        User user = new User();
        user.setUsername(extractJsonValue(body, "username"));
        user.setPassword(extractJsonValue(body, "password"));
        user.setFullName(extractJsonValue(body, "fullName"));
        user.setEmail(extractJsonValue(body, "email"));
        user.setAccountType(extractJsonValue(body, "accountType"));
        user.setVpa(extractJsonValue(body, "username") + "@fincore"); // Auto-generate VPA
        user.setBalance(new BigDecimal("0.00"));

        if (userDAO.register(user)) {
            response.setBody("{\"status\": \"success\", \"message\": \"Registration successful. Pending approval.\"}");
        } else {
            response.setStatus(400, "Bad Request");
            response.setBody("{\"error\": \"Registration failed. Username might be taken.\"}");
        }
    }

    // Helper for simple JSON parsing
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        
        char firstChar = json.charAt(start);
        while (firstChar == ' ' || firstChar == '"') {
            start++;
            firstChar = json.charAt(start);
        }
        
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
