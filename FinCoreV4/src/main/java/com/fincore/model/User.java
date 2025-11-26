package com.fincore.model;

import java.math.BigDecimal;

public class User {
    private int id;
    private String username;
    private String password;
    private String vpa;
    private BigDecimal balance;
    private String role;
    private String status;
    private String fullName;
    private String email;
    private String accountType;

    public User() {}

    public User(int id, String username, String password, String vpa, BigDecimal balance, 
                String role, String status, String fullName, String email, String accountType) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.vpa = vpa;
        this.balance = balance;
        this.role = role;
        this.status = status;
        this.fullName = fullName;
        this.email = email;
        this.accountType = accountType;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getVpa() { return vpa; }
    public void setVpa(String vpa) { this.vpa = vpa; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
}
