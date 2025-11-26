import java.math.BigDecimal;

/**
 * THE MODEL
 * Updated to include security credentials.
 */
public class Account {
    private String id;
    private String owner;
    private BigDecimal balance;
    private String passwordHash; // New Field

    public Account(String id, String owner, BigDecimal balance, String passwordHash) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
        this.passwordHash = passwordHash;
    }

    // Synchronized to ensure thread safety at the entity level
    public synchronized void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public synchronized boolean withdraw(BigDecimal amount) {
        if (balance.compareTo(amount) >= 0) {
            balance = balance.subtract(amount);
            return true;
        }
        return false;
    }

    public String getId() { return id; }
    public BigDecimal getBalance() { return balance; }
    public String getOwner() { return owner; }
    public String getPasswordHash() { return passwordHash; }
}