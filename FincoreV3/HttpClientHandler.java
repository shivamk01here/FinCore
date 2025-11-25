import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * THE CONTROLLER / NETWORK LAYER
 * 1. Reads raw bytes from the socket.
 * 2. Parses HTTP string.
 * 3. Calls the Service Layer.
 * 4. Formats the JSON response.
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

            // 1. Read Request
            String requestLine = in.readLine();
            if (requestLine == null) return;
            System.out.println("Request: " + requestLine);

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return; 
            
            String method = parts[0]; 
            String fullPath = parts[1]; 

            // 2. Parse Query Params
            String path = fullPath;
            Map<String, String> queryParams = new HashMap<>();
            
            if (fullPath.contains("?")) {
                String[] pathParts = fullPath.split("\\?");
                path = pathParts[0];
                if (pathParts.length > 1) {
                    for (String param : pathParts[1].split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length == 2) {
                            queryParams.put(pair[0], pair[1]);
                        }
                    }
                }
            }

            // 3. Route & Execute
            String responseBody = "";
            int statusCode = 200;

            try {
                if (method.equals("GET") && path.equals("/api/balance")) {
                    responseBody = handleGetBalance(queryParams);
                } 
                else if ((method.equals("GET") || method.equals("POST")) && path.equals("/api/transfer")) {
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

            // 4. Send Response
            String httpResponse = "HTTP/1.1 " + statusCode + " OK\r\n" +
                                  "Content-Type: application/json\r\n" +
                                  "Access-Control-Allow-Origin: *\r\n" +
                                  "Content-Length: " + responseBody.length() + "\r\n" +
                                  "\r\n" +
                                  responseBody;

            out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private String handleGetBalance(Map<String, String> params) throws Exception {
        String id = params.get("id");
        if (id == null) throw new Exception("Missing id parameter");

        Account acc = service.getAccount(id);
        if (acc == null) throw new Exception("Account not found");

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