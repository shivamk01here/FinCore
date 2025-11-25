import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


// ---------------------------------------------------------
// 1. EXCEPTIONS
// Custom exceptions allow us to handle logic errors gracefully
// ---------------------------------------------------------
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) {
        super(message);
    }
}


// ---------------------------------------------------------
// 2. ENUMS & UTILS
// Defining fixed types ensures type safety across the system
// ---------------------------------------------------------
enum TransactionType {
    DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
}

enum Currency {
    USD, EUR, INR, GBP
}



// ---------------------------------------------------------
// 3. DOMAIN MODELS (The "Entity" Layer)
// ---------------------------------------------------------

/**
 * IMMUTABILITY PATTERN:
 * A Transaction should never change once executed. 
 * Notice all fields are final and there are no setters.
 */
class Transaction {
    private final String transactionId;
    private final LocalDateTime timestamp;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String reference; // e.g., "Transfer to Acc 102"

    public Transaction(TransactionType type, BigDecimal amount, String reference) {
        this.transactionId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.amount = amount;
        this.reference = reference;
    }

    @Override
    public String toString() {
        return String.format("[%s] %-12s | %10s | %s", 
            timestamp.format(DateTimeFormatter.ISO_LOCAL_TIME), 
            type, 
            amount.setScale(2, RoundingMode.HALF_EVEN), 
            reference);
    }
}

/**
 * ABSTRACTION PATTERN:
 * 'Account' is abstract. You can't have just an "Account".
 * It must be a "SavingsAccount" or "CheckingAccount".
 */
abstract class Account {
    // Encapsulation: strictly private fields
    private final String accountNumber;
    private final String ownerName;
    protected BigDecimal balance; // protected so subclasses can access, but risky. Better to use setters.
    private final List<Transaction> transactionHistory;
    private final Currency currency;

    public Account(String accountNumber, String ownerName, Currency currency) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.currency = currency;
        this.balance = BigDecimal.ZERO;
        this.transactionHistory = new ArrayList<>();
    }

    // Abstract method: Subclasses MUST define their own rules for interest/fees
    public abstract void applyEndOfMonthProcessing();

    public synchronized void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balance = balance.add(amount);
        addTransaction(new Transaction(TransactionType.DEPOSIT, amount, "ATM Deposit"));
    }

    public synchronized void withdraw(BigDecimal amount) throws InsufficientFundsException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account " + accountNumber);
        }
        balance = balance.subtract(amount);
        addTransaction(new Transaction(TransactionType.WITHDRAWAL, amount.negate(), "ATM Withdrawal"));
    }

    // Internal transfer logic
    protected synchronized void receiveTransfer(BigDecimal amount, String fromAccount) {
        balance = balance.add(amount);
        addTransaction(new Transaction(TransactionType.TRANSFER_IN, amount, "From: " + fromAccount));
    }

    protected synchronized void sendTransfer(BigDecimal amount, String toAccount) throws InsufficientFundsException {
         if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds for transfer");
        }
        balance = balance.subtract(amount);
        addTransaction(new Transaction(TransactionType.TRANSFER_OUT, amount.negate(), "To: " + toAccount));
    }

    private void addTransaction(Transaction t) {
        transactionHistory.add(t);
    }

    public BigDecimal getBalance() { return balance; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }

    public void printStatement() {
        System.out.println("\n--- STATEMENT FOR: " + ownerName + " (" + accountNumber + ") ---");
        System.out.println("Current Balance: " + currency + " " + balance.setScale(2, RoundingMode.HALF_EVEN));
        System.out.println("----------------------------------------------------------");
        for (Transaction t : transactionHistory) {
            System.out.println(t);
        }
        System.out.println("----------------------------------------------------------\n");
    }
}

/**
 * POLYMORPHISM:
 * A SavingsAccount behaves differently than a CheckingAccount
 */
class SavingsAccount extends Account {
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.03"); // 3% interest

    public SavingsAccount(String accountNumber, String ownerName, Currency currency) {
        super(accountNumber, ownerName, currency);
    }

    @Override
    public void applyEndOfMonthProcessing() {
        BigDecimal interest = balance.multiply(INTEREST_RATE);
        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            deposit(interest);
            // Note: In a real system we'd mark this transaction specifically as INTEREST
            System.out.println("Interest applied to " + getAccountNumber() + ": " + interest);
        }
    }
}

class CheckingAccount extends Account {
    private static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("500.00");
    private static final BigDecimal MAINTENANCE_FEE = new BigDecimal("12.00");

    public CheckingAccount(String accountNumber, String ownerName, Currency currency) {
        super(accountNumber, ownerName, currency);
    }
    
    // Overriding withdraw to allow overdraft
    @Override
    public synchronized void withdraw(BigDecimal amount) throws InsufficientFundsException {
        BigDecimal maxWithdrawable = balance.add(OVERDRAFT_LIMIT);
        if (maxWithdrawable.compareTo(amount) < 0) {
             throw new InsufficientFundsException("Overdraft limit exceeded");
        }
        // Logic simplifies here, normally we handle negative balance logic carefully
        balance = balance.subtract(amount); 
        // We use reflection or protected access carefully here, 
        // ideally Transaction adding should be via a protected method in parent.
        // For this demo, we assume the parent logic handles the subtraction correctly even if negative.
    }

    @Override
    public void applyEndOfMonthProcessing() {
        try {
            // Hard withdrawal for fees
            balance = balance.subtract(MAINTENANCE_FEE); 
            System.out.println("Maintenance fee charged to " + getAccountNumber());
        } catch (Exception e) {
            // In real banking, fees can make you go negative
        }
    }
}

// ---------------------------------------------------------
// 4. SERVICE LAYER
// This acts as the "API" logic before we have HTTP
// ---------------------------------------------------------
class BankingService {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>(); // In-memory storage for simplicity
    private final AtomicLong accountIdGenerator = new AtomicLong(1000); 

    public Account createAccount(String type, String owner, Currency currency) {
        String accountNumber = String.valueOf(accountIdGenerator.getAndIncrement());
        Account newAccount;

        if (type.equalsIgnoreCase("SAVINGS")) {
            newAccount = new SavingsAccount(accountNumber, owner, currency);
        } else if (type.equalsIgnoreCase("CHECKING")) {
            newAccount = new CheckingAccount(accountNumber, owner, currency);
        } else {
            throw new IllegalArgumentException("Unknown account type");
        }

        accounts.put(accountNumber, newAccount);
        System.out.println("Created " + type + " account " + accountNumber + " for " + owner);
        return newAccount;
    }

    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        Account acc = accounts.get(accountNumber);
        if (acc == null) throw new AccountNotFoundException("Account " + accountNumber + " not found.");
        return acc;
    }

    // ATOMIC TRANSFER
    // This is critical. Transfers must be atomic. If the deposit fails, 
    // the withdrawal must effectively be rolled back (or never happen).
    public void transfer(String fromAccNum, String toAccNum, BigDecimal amount) 
            throws AccountNotFoundException, InsufficientFundsException {
        
        Account from = getAccount(fromAccNum);
        Account to = getAccount(toAccNum);

        // LOCKING ORDER to prevent Deadlocks:
        // Always lock the account with the smaller hashcode/ID first.
        Account firstLock = fromAccNum.compareTo(toAccNum) < 0 ? from : to;
        Account secondLock = fromAccNum.compareTo(toAccNum) < 0 ? to : from;

        synchronized (firstLock) {
            synchronized (secondLock) {
                from.sendTransfer(amount, toAccNum);
                to.receiveTransfer(amount, fromAccNum);
            }
        }
        System.out.println("Transfer successful: " + amount + " from " + fromAccNum + " to " + toAccNum);
    }
}



// ---------------------------------------------------------
// 5. MAIN EXECUTION (The "Frontend" for now)
// ---------------------------------------------------------
public class CoreBankingSystem {
    public static void main(String[] args) {
        System.out.println("Initializing Core Banking System...");
        BankingService bank = new BankingService();

        try {
            // 1. Create Users
            Account alice = bank.createAccount("SAVINGS", "Alice Engineer", Currency.USD);
            Account bob = bank.createAccount("CHECKING", "Bob Builder", Currency.USD);

            // 2. Initial Deposits
            alice.deposit(new BigDecimal("1000.00"));
            bob.deposit(new BigDecimal("500.00"));

            // 3. Failed Withdrawal (Logic Check)
            try {
                alice.withdraw(new BigDecimal("2000.00"));
            } catch (InsufficientFundsException e) {
                System.out.println("Expected Error: " + e.getMessage());
            }

            // 4. Transfer Logic
            bank.transfer(alice.getAccountNumber(), bob.getAccountNumber(), new BigDecimal("250.00"));

            // 5. End of Month Processing (Interest/Fees)
            System.out.println("\nRunning End of Month Batch...");
            alice.applyEndOfMonthProcessing();
            bob.applyEndOfMonthProcessing();

            // 6. Print Statements
            alice.printStatement();
            bob.printStatement();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}