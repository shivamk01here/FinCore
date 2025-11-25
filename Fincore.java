import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// ---------------------------------------------------------
// 1. EXCEPTIONS
// ---------------------------------------------------------
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) { super(message); }
}

class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) { super(message); }
}

// ---------------------------------------------------------
// 2. ENUMS & UTILS
// ---------------------------------------------------------
enum TransactionType {
    DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
}

enum Currency {
    USD, EUR, INR, GBP
}

// ---------------------------------------------------------
// 3. DOMAIN MODELS (Entities)
// ---------------------------------------------------------
class Transaction {
    private final String transactionId;
    private final LocalDateTime timestamp;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String reference;

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

abstract class Account {
    private final String accountNumber;
    private final String ownerName;
    protected BigDecimal balance;
    private final List<Transaction> transactionHistory;
    private final Currency currency;

    public Account(String accountNumber, String ownerName, Currency currency) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.currency = currency;
        this.balance = BigDecimal.ZERO;
        this.transactionHistory = new ArrayList<>();
    }

    public abstract void applyEndOfMonthProcessing();

    public synchronized void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) 
            throw new IllegalArgumentException("Deposit amount must be positive");
        
        balance = balance.add(amount);
        addTransaction(new Transaction(TransactionType.DEPOSIT, amount, "ATM Deposit"));
    }

    public synchronized void withdraw(BigDecimal amount) throws InsufficientFundsException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) 
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        
        if (balance.compareTo(amount) < 0) 
            throw new InsufficientFundsException("Insufficient funds in account " + accountNumber);
        
        balance = balance.subtract(amount);
        addTransaction(new Transaction(TransactionType.WITHDRAWAL, amount.negate(), "ATM Withdrawal"));
    }
    
    protected synchronized void receiveTransfer(BigDecimal amount, String fromAccount) {
        balance = balance.add(amount);
        addTransaction(new Transaction(TransactionType.TRANSFER_IN, amount, "From: " + fromAccount));
    }
    
    protected synchronized void sendTransfer(BigDecimal amount, String toAccount) throws InsufficientFundsException {
         if (balance.compareTo(amount) < 0) 
            throw new InsufficientFundsException("Insufficient funds for transfer");
        
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

class SavingsAccount extends Account {
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.03");

    public SavingsAccount(String accountNumber, String ownerName, Currency currency) {
        super(accountNumber, ownerName, currency);
    }

    @Override
    public void applyEndOfMonthProcessing() {
        BigDecimal interest = balance.multiply(INTEREST_RATE);
        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            deposit(interest);
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
    
    @Override
    public synchronized void withdraw(BigDecimal amount) throws InsufficientFundsException {
        BigDecimal maxWithdrawable = balance.add(OVERDRAFT_LIMIT);
        if (maxWithdrawable.compareTo(amount) < 0) {
             throw new InsufficientFundsException("Overdraft limit exceeded");
        }
        balance = balance.subtract(amount); 
    }

    @Override
    public void applyEndOfMonthProcessing() {
        try {
            balance = balance.subtract(MAINTENANCE_FEE); 
            System.out.println("Maintenance fee charged to " + getAccountNumber());
        } catch (Exception e) {}
    }
}

// ---------------------------------------------------------
// 4. REPOSITORY LAYER (The "Persistence" Interface)
// Separation of Concerns: This layer only cares about storing data.
// ---------------------------------------------------------
interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(String accountNumber);
    List<Account> findAll();
}

/**
 * In-Memory Implementation.
 * Later, we can swap this class with 'JdbcAccountRepository' without changing the Service!
 */
class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> store = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        store.put(account.getAccountNumber(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(String accountNumber) {
        return Optional.ofNullable(store.get(accountNumber));
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(store.values());
    }
}

// ---------------------------------------------------------
// 5. SERVICE LAYER (Business Logic)
// ---------------------------------------------------------
class BankingService {
    // DEPENDENCY INJECTION PATTERN:
    // The service doesn't know *how* accounts are stored, it just asks the repository.
    private final AccountRepository accountRepository;
    private final AtomicLong accountIdGenerator = new AtomicLong(1000);

    // Constructor Injection
    public BankingService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

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

        return accountRepository.save(newAccount);
    }

    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        return accountRepository.findById(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException("Account " + accountNumber + " not found"));
    }

    public void transfer(String fromAccNum, String toAccNum, BigDecimal amount) 
            throws AccountNotFoundException, InsufficientFundsException {
        
        Account from = getAccount(fromAccNum);
        Account to = getAccount(toAccNum);

        // Lock ordering (same as before)
        Account firstLock = fromAccNum.compareTo(toAccNum) < 0 ? from : to;
        Account secondLock = fromAccNum.compareTo(toAccNum) < 0 ? to : from;

        synchronized (firstLock) {
            synchronized (secondLock) {
                from.sendTransfer(amount, toAccNum);
                to.receiveTransfer(amount, fromAccNum);
            }
        }
        // In a real DB, we would call accountRepository.save(from) here to persist changes
        System.out.println("Transfer successful: " + amount + " from " + fromAccNum + " to " + toAccNum);
    }

    public void runEndOfMonthBatch() {
        System.out.println("\nRunning End of Month Batch...");
        for (Account acc : accountRepository.findAll()) {
            acc.applyEndOfMonthProcessing();
        }
    }
}

// ---------------------------------------------------------
// 6. MAIN EXECUTION (Fincore App)
// ---------------------------------------------------------
public class FincoreApp {
    public static void main(String[] args) {
        System.out.println("Initializing Fincore Banking System...");

        // 1. Setup Dependencies (Manual Dependency Injection)
        AccountRepository repository = new InMemoryAccountRepository();
        BankingService fincoreService = new BankingService(repository);

        try {
            // 2. Create Users
            Account alice = fincoreService.createAccount("SAVINGS", "Alice Engineer", Currency.USD);
            Account bob = fincoreService.createAccount("CHECKING", "Bob Builder", Currency.USD);

            System.out.println("Created Accounts: " + alice.getAccountNumber() + ", " + bob.getAccountNumber());

            // 3. Transactions
            alice.deposit(new BigDecimal("1000.00"));
            bob.deposit(new BigDecimal("500.00"));

            fincoreService.transfer(alice.getAccountNumber(), bob.getAccountNumber(), new BigDecimal("250.00"));

            // 4. Batch Processing
            fincoreService.runEndOfMonthBatch();

            // 5. Reporting
            alice.printStatement();
            bob.printStatement();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}