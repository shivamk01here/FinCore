import java.math.BigDecimal;

/**
 * THE MODEL
 * Represents the core business entity.
 * It is isolated from the Web, the Database, and the Service logic.
 */
public class Account {
    private String id;
    private String owner;
    private BigDecimal balance;

    public Account(String id, String owner, BigDecimal balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
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
}