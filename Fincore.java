import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// --- REUSING DOMAIN & REPOSITORY (Simplified for single file) ---
// (Normally these are in separate files, I am including the minimal parts needed to run)

class Account {
    private String id;
    private String owner;
    private BigDecimal balance;
    
    public Account(String id, String owner, BigDecimal balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
    }
    
    public synchronized void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
    
    public synchronized boolean withdraw(BigDecimal amount) {
        if(balance.compareTo(amount) >= 0) {
            balance = balance.subtract(amount);
            return true;
        }
        return false;
    }

    public String getId() { return id; }
    public BigDecimal getBalance() { return balance; }
    public String getOwner() { return owner; }
}

class BankingService {
    private Map<String, Account> accounts = new ConcurrentHashMap<>();
    
    public BankingService() {
        // Seed some data so we can test immediately
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
        
        synchronized(from) { // Simple locking for demo
            if (from.withdraw(amount)) {
                to.deposit(amount);
            } else {
                throw new Exception("Insufficient funds");
            }
        }
    }
}

// ---------------------------------------------------------
// THE HTTP SERVER CORE
// ---------------------------------------------------------

class HttpClientHandler implements Runnable {
    private Socket socket;
    private BankingService service;

    public HttpClientHandler(Socket socket, BankingService service) {
        this.socket = socket;
        this.service = service;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            // 1. Read the Request Line (e.g., "GET /api/balance?id=1001 HTTP/1.1")
            String requestLine = in.readLine();
            if (requestLine == null) return;
            System.out.println("Request: " + requestLine);

            String[] parts = requestLine.split(" ");
            String method = parts[0]; // GET, POST
            String fullPath = parts[1]; // /api/balance?id=1001

            // 2. Parse Path and Query Parameters manually
            String path = fullPath;
            Map<String, String> queryParams = new HashMap<>();
            
            if (fullPath.contains("?")) {
                String[] pathParts = fullPath.split("\\?");
                path = pathParts[0];
                String query = pathParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        queryParams.put(pair[0], pair[1]);
                    }
                }
            }

            // 3. Route the Request (The API Layer)
            String responseBody = "";
            int statusCode = 200;

            try {
                if (method.equals("GET") && path.equals("/api/balance")) {
                    responseBody = handleGetBalance(queryParams);
                } 
                else if (method.equals("POST") && path.equals("/api/transfer")) {
                    // For POST, we usually read the Body. 
                    // For simplicity in this step, we will use Query Params on POST too 
                    // (Not standard, but easier for Phase 1 of HTTP learning)
                    responseBody = handleTransfer(queryParams);
                } 
                else {
                    statusCode = 404;
                    responseBody = "{ \"error\": \"Endpoint not found\" }";
                }
            } catch (Exception e) {
                statusCode = 500;
                responseBody = "{ \"error\": \"" + e.getMessage() + "\" }";
            }

            // 4. Send the HTTP Response
            // This is the raw text the browser expects
            String httpResponse = "HTTP/1.1 " + statusCode + " OK\r\n" +
                                  "Content-Type: application/json\r\n" +
                                  "Access-Control-Allow-Origin: *\r\n" + // Allows frontend connection later
                                  "Content-Length: " + responseBody.length() + "\r\n" +
                                  "\r\n" + // Blank line separates headers from body
                                  responseBody;

            out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

    // --- API HANDLERS ---

    private String handleGetBalance(Map<String, String> params) throws Exception {
        String id = params.get("id");
        if (id == null) throw new Exception("Missing id parameter");

        Account acc = service.getAccount(id);
        if (acc == null) throw new Exception("Account not found");

        // Manually building JSON
        return String.format("{ \"id\": \"%s\", \"owner\": \"%s\", \"balance\": %s }", 
                             acc.getId(), acc.getOwner(), acc.getBalance());
    }

    private String handleTransfer(Map<String, String> params) throws Exception {
        String from = params.get("from");
        String to = params.get("to");
        String amtStr = params.get("amount");

        if (from == null || to == null || amtStr == null) 
            throw new Exception("Missing parameters");

        service.transfer(from, to, new BigDecimal(amtStr));

        return "{ \"status\": \"success\", \"message\": \"Transfer completed\" }";
    }
}

public class FincoreHttpServer {
    public static void main(String[] args) throws IOException {
        int PORT = 8080;
        ServerSocket server = new ServerSocket(PORT);
        BankingService service = new BankingService();

        System.out.println("Fincore HTTP Server running on http://localhost:" + PORT);
        
        while (true) {
            Socket client = server.accept();
            new Thread(new HttpClientHandler(client, service)).start();
        }
    }
}