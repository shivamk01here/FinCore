import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * THE SERVICE LAYER
 * Contains the business rules (e.g., "User A sends money to User B").
 * It doesn't know about HTTP or JSON.
 */
public class BankingService {
    // In a real app, this Map would be replaced by a Repository/Database connection
    private Map<String, Account> accounts = new ConcurrentHashMap<>();

    public BankingService() {
        // Seed dummy data
        accounts.put("1001", new Account("1001", "Alice", new BigDecimal("1000.00")));
        accounts.put("1002", new Account("1002", "Bob", new BigDecimal("500.00")));
    }

    public Account getAccount(String id) {
        return accounts.get(id);
    }

    public void transfer(String fromId, String toId, BigDecimal amount) throws Exception {
        Account from = accounts.get(fromId);
        Account to = accounts.get(toId);
        
        if (from == null || to == null) throw new Exception("Account not found");

        // Simple locking strategy to prevent race conditions during transfer
        synchronized(from) {
            if (from.withdraw(amount)) {
                to.deposit(amount);
            } else {
                throw new Exception("Insufficient funds");
            }
        }
    }
}