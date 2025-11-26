package com.fincore.model;

import java.sql.Timestamp;

public class Notification {
    private int id;
    private int userId;
    private String message;
    private boolean readStatus;
    private Timestamp timestamp;

    public Notification() {}

    public Notification(int id, int userId, String message, boolean readStatus, Timestamp timestamp) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.readStatus = readStatus;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isReadStatus() { return readStatus; }
    public void setReadStatus(boolean readStatus) { this.readStatus = readStatus; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
