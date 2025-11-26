import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * THE CONTROLLER LAYER
 * Updated to handle Authentication Tokens.
 */
public class HttpClientHandler implements Runnable {
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

            String requestLine = in.readLine();
            if (requestLine == null) return;
            System.out.println("Request: " + requestLine);

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return; 
            
            String method = parts[0]; 
            String fullPath = parts[1]; 

            // Parse Query Params (Keeping it simple via URL params for now)
            Map<String, String> params = parseParams(fullPath);
            String path = fullPath.split("\\?")[0];

            String responseBody = "";
            int statusCode = 200;

            try {
                // --- PUBLIC ROUTES ---
                if (path.equals("/api/register") && method.equals("POST")) {
                    responseBody = handleRegister(params);
                }
                else if (path.equals("/api/login") && method.equals("POST")) {
                    responseBody = handleLogin(params);
                }
                // --- PROTECTED ROUTES (Require Token) ---
                else if (path.equals("/api/balance") && method.equals("GET")) {
                    validateToken(params);
                    responseBody = handleGetBalance(params);
                } 
                else if (path.equals("/api/transfer") && method.equals("POST")) {
                    validateToken(params);
                    responseBody = handleTransfer(params);
                } 
                else {
                    statusCode = 404;
                    responseBody = "{ \"error\": \"Endpoint not found\" }";
                }
            } catch (Exception e) {
                statusCode = 500; // or 401/403 based on error
                responseBody = "{ \"error\": \"" + e.getMessage() + "\" }";
            }

            sendResponse(out, statusCode, responseBody);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

    // --- HANDLERS ---

    private String handleRegister(Map<String, String> params) throws Exception {
        String user = params.get("user");
        String pass = params.get("pass");
        String bal = params.get("initial_balance");
        
        if (user == null || pass == null) throw new Exception("Missing user/pass");
        
        Account acc = service.register(user, pass, new BigDecimal(bal != null ? bal : "0"));
        return String.format("{ \"message\": \"Registered\", \"id\": \"%s\" }", acc.getId());
    }

    private String handleLogin(Map<String, String> params) throws Exception {
        String id = params.get("id");
        String pass = params.get("pass");
        
        String token = service.login(id, pass);
        return String.format("{ \"status\": \"logged_in\", \"token\": \"%s\" }", token);
    }

    private String handleGetBalance(Map<String, String> params) throws Exception {
        // We get the ID *from the token*, not from the user params (Security!)
        String token = params.get("token");
        String userId = service.getUserIdFromToken(token);
        
        Account acc = service.getAccount(userId);
        return String.format("{ \"id\": \"%s\", \"owner\": \"%s\", \"balance\": %s }", 
                             acc.getId(), acc.getOwner(), acc.getBalance());
    }

    private String handleTransfer(Map<String, String> params) throws Exception {
        String token = params.get("token");
        String fromId = service.getUserIdFromToken(token); // Authenticated User
        String toId = params.get("to");
        String amtStr = params.get("amount");

        service.transfer(fromId, toId, new BigDecimal(amtStr));
        return "{ \"status\": \"success\", \"message\": \"Transfer completed\" }";
    }

    // --- HELPERS ---

    private void validateToken(Map<String, String> params) throws Exception {
        if (!params.containsKey("token")) throw new Exception("Unauthorized: Missing token");
        // Service check happens inside specific handlers to get UserID
    }

    private Map<String, String> parseParams(String fullPath) {
        Map<String, String> queryParams = new HashMap<>();
        if (fullPath.contains("?")) {
            String[] pathParts = fullPath.split("\\?");
            if (pathParts.length > 1) {
                for (String param : pathParts[1].split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        queryParams.put(pair[0], pair[1]);
                    }
                }
            }
        }
        return queryParams;
    }

    private void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        String httpResponse = "HTTP/1.1 " + statusCode + " OK\r\n" +
                              "Content-Type: application/json\r\n" +
                              "Access-Control-Allow-Origin: *\r\n" +
                              "Content-Length: " + body.length() + "\r\n" +
                              "\r\n" +
                              body;
        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}