import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * MAIN ENTRY POINT
 * Bootstraps the application.
 */
public class FincoreServer {
    public static void main(String[] args) throws IOException {
        int PORT = 8080;
        
        // 1. Initialize the Service Layer
        BankingService service = new BankingService();
        
        // 2. Start the Server
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Fincore HTTP Server running on http://localhost:" + PORT);
        
        while (true) {
            Socket client = server.accept();
            // 3. Inject Service into the Handler (Dependency Injection)
            new Thread(new HttpClientHandler(client, service)).start();
        }
    }
}