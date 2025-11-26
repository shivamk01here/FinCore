CREATE DATABASE IF NOT EXISTS test;
USE test;

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    vpa VARCHAR(50) NOT NULL UNIQUE,
    balance DECIMAL(10, 2) DEFAULT 0.00,
    role VARCHAR(20) DEFAULT 'USER', -- 'ADMIN', 'USER'
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'ACTIVE'
    full_name VARCHAR(100),
    email VARCHAR(100),
    account_type VARCHAR(50) -- 'SAVINGS', 'CURRENT'
);

CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT,
    receiver_id INT,
    amount DECIMAL(10, 2) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);

CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    message TEXT NOT NULL,
    read_status BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Insert Admin User
INSERT INTO users (username, password, vpa, balance, role, status, full_name, email, account_type) 
VALUES ('admin', 'admin123', 'admin@fincore', 0.00, 'ADMIN', 'ACTIVE', 'System Admin', 'admin@fincore.com', 'SYSTEM');

-- Insert a test user (Active)
INSERT INTO users (username, password, vpa, balance, role, status, full_name, email, account_type) 
VALUES ('user1', 'pass123', 'user1@fincore', 5000.00, 'USER', 'ACTIVE', 'Test User 1', 'user1@test.com', 'SAVINGS');
