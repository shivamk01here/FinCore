import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
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
    
    // Returns string representation for network response
    public String getStatementString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- STATEMENT FOR: ").append(ownerName).append(" (" ).append(accountNumber).append(") ---\n");
        sb.append("Current Balance: ").append(currency).append(" ").append(balance.setScale(2, RoundingMode.HALF_EVEN)).append("\n");
        for (Transaction t : transactionHistory) {
            sb.append(t.toString()).append("\n");
        }
        return sb.toString();
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
        } catch (Exception e) {}
    }
}

// ---------------------------------------------------------
// 4. REPOSITORY LAYER
// ---------------------------------------------------------
interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(String accountNumber);
    List<Account> findAll();
}

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
// 5. SERVICE LAYER
// ---------------------------------------------------------
class BankingService {
    private final AccountRepository accountRepository;
    private final AtomicLong accountIdGenerator = new AtomicLong(1000);

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

        Account firstLock = fromAccNum.compareTo(toAccNum) < 0 ? from : to;
        Account secondLock = fromAccNum.compareTo(toAccNum) < 0 ? to : from;

        synchronized (firstLock) {
            synchronized (secondLock) {
                from.sendTransfer(amount, toAccNum);
                to.receiveTransfer(amount, fromAccNum);
            }
        }
    }
}

// ---------------------------------------------------------
// 6. NETWORK LAYER (New Custom TCP Server)
// ---------------------------------------------------------

/**
 * Handles individual client connections in a separate thread.
 * This interprets raw strings (our protocol) and calls the BankingService.
 */
class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final BankingService bankingService;

    public ClientHandler(Socket socket, BankingService service) {
        this.clientSocket = socket;
        this.bankingService = service;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            out.println("WELCOME TO FINCORE BANKING SYSTEM v2.0");
            out.println("Commands: CREATE <type> <name> <curr>, DEPOSIT <id> <amt>, TRANSFER <from> <to> <amt>, BALANCE <id>");
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    String response = processCommand(inputLine);
                    out.println("OK: " + response);
                } catch (Exception e) {
                    out.println("ERROR: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private String processCommand(String commandLine) throws Exception {
        String[] parts = commandLine.trim().split("\\s+");
        if (parts.length == 0) return "";
        
        String command = parts[0].toUpperCase();

        switch (command) {
            case "CREATE":
                // Usage: CREATE SAVINGS Alice USD
                if (parts.length < 4) throw new IllegalArgumentException("Usage: CREATE <TYPE> <NAME> <CURRENCY>");
                String type = parts[1];
                String name = parts[2]; // Limitation: Name cannot have spaces currently
                Currency curr = Currency.valueOf(parts[3].toUpperCase());
                Account newAcc = bankingService.createAccount(type, name, curr);
                return "Account created ID: " + newAcc.getAccountNumber();

            case "DEPOSIT":
                // Usage: DEPOSIT 1001 500.00
                if (parts.length < 3) throw new IllegalArgumentException("Usage: DEPOSIT <ID> <AMOUNT>");
                Account depAcc = bankingService.getAccount(parts[1]);
                BigDecimal depAmt = new BigDecimal(parts[2]);
                depAcc.deposit(depAmt);
                return "New Balance: " + depAcc.getBalance();

            case "TRANSFER":
                // Usage: TRANSFER 1001 1002 100.00
                if (parts.length < 4) throw new IllegalArgumentException("Usage: TRANSFER <FROM> <TO> <AMOUNT>");
                bankingService.transfer(parts[1], parts[2], new BigDecimal(parts[3]));
                return "Transfer Successful";

            case "BALANCE":
                 // Usage: BALANCE 1001
                 if (parts.length < 2) throw new IllegalArgumentException("Usage: BALANCE <ID>");
                 Account balAcc = bankingService.getAccount(parts[1]);
                 return "\n" + balAcc.getStatementString();

            default:
                throw new IllegalArgumentException("Unknown Command: " + command);
        }
    }
}

// ---------------------------------------------------------
// 7. MAIN APP (Starts the Server)
// ---------------------------------------------------------
public class FincoreApp {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("Starting Fincore Server on port " + PORT + "...");

        // 1. Initialize Core Layers
        AccountRepository repository = new InMemoryAccountRepository();
        BankingService bankingService = new BankingService(repository);

        // 2. Start TCP Server
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running. Waiting for connections...");

            while (true) {
                // BLOCKING CALL: Waits here until a client connects
                Socket clientSocket = serverSocket.accept();
                System.out.println("New Client Connected: " + clientSocket.getInetAddress());

                // Spawn a new Thread for this client
                ClientHandler handler = new ClientHandler(clientSocket, bankingService);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}