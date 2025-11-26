package com.fincore.server;

import com.fincore.handler.AuthHandler;
import com.fincore.handler.DashboardHandler;
import com.fincore.handler.TransactionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleHttpServer {
    private static final int PORT = 8080;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             clientSocket) {
            
            HttpRequest request = new HttpRequest(reader);
            HttpResponse response = new HttpResponse(clientSocket.getOutputStream());

            if (request.getPath() == null) return;

            System.out.println(request.getMethod() + " " + request.getPath());

            // Routing
            if (request.getPath().startsWith("/api/auth")) {
                new AuthHandler().handle(request, response);
            } else if (request.getPath().startsWith("/api/dashboard") || request.getPath().startsWith("/api/admin")) {
                new DashboardHandler().handle(request, response);
            } else if (request.getPath().startsWith("/api/transaction")) {
                new TransactionHandler().handle(request, response);
            } else {
                new StaticFileHandler().handle(request, response);
            }

            response.send();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
