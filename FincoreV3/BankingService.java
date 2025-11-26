import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * THE SERVICE LAYER
 * Now handles:
 * 1. SQL Persistence
 * 2. Session Management (Token -> UserID)
 */
public class BankingService {
    
    // In-memory Session Store: Maps "AuthToken" -> "AccountID"
    // If the server restarts, everyone is logged out (standard security practice)
    private Map<String, String> activeSessions = new ConcurrentHashMap<>();

    public BankingService() {
        // Initialize DB tables
        Database.initialize();
    }

    // --- AUTHENTICATION ---

    public Account register(String owner, String password, BigDecimal initialBalance) throws Exception {
        String id = String.valueOf(System.currentTimeMillis() % 100000); // Simple ID generation
        String hashedPassword = String.valueOf(password.hashCode()); // basics hashing (Use BCrypt in prod)

        String sql = "INSERT INTO accounts (id, owner, balance, password_hash) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            pstmt.setString(2, owner);
            pstmt.setBigDecimal(3, initialBalance);
            pstmt.setString(4, hashedPassword);
            pstmt.executeUpdate();
            
            return new Account(id, owner, initialBalance, hashedPassword);
        }
    }

    public String login(String id, String password) throws Exception {
        Account acc = getAccount(id);
        if (acc == null) throw new Exception("User not found");
        
        String inputHash = String.valueOf(password.hashCode());
        
        if (!acc.getPasswordHash().equals(inputHash)) {
            throw new Exception("Invalid credentials");
        }

        // Generate Session Token
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, acc.getId());
        return token;
    }

    public String getUserIdFromToken(String token) throws Exception {
        if (token == null || !activeSessions.containsKey(token)) {
            throw new Exception("Invalid or expired session token");
        }
        return activeSessions.get(token);
    }

    // --- CORE BANKING (SQL VERSION) ---

    public Account getAccount(String id) throws Exception {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Account(
                    rs.getString("id"),
                    rs.getString("owner"),
                    rs.getBigDecimal("balance"),
                    rs.getString("password_hash")
                );
            }
        }
        return null;
    }

    public void transfer(String fromId, String toId, BigDecimal amount) throws Exception {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new Exception("Invalid amount");

        // Transactional Transfer
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); // Start Transaction

            try {
                // 1. Check Balance
                // Note: In real SQL, we would use "SELECT ... FOR UPDATE" to lock rows
                Account from = getAccount(fromId);
                Account to = getAccount(toId);

                if (from == null || to == null) throw new Exception("Account not found");
                if (from.getBalance().compareTo(amount) < 0) throw new Exception("Insufficient funds");

                // 2. Perform Updates
                String updateSql = "UPDATE accounts SET balance = ? WHERE id = ?";
                
                try (PreparedStatement sub = conn.prepareStatement(updateSql)) {
                    // Deduct
                    sub.setBigDecimal(1, from.getBalance().subtract(amount));
                    sub.setString(2, fromId);
                    sub.executeUpdate();

                    // Add
                    sub.setBigDecimal(1, to.getBalance().add(amount));
                    sub.setString(2, toId);
                    sub.executeUpdate();
                }

                conn.commit(); // Commit Transaction
            } catch (Exception e) {
                conn.rollback(); // Rollback on error
                throw e;
            }
        }
    }
}