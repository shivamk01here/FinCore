package com.fincore.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class StaticFileHandler {
    private static final String WEB_ROOT = "frontend";

    public void handle(HttpRequest request, HttpResponse response) {
        String path = request.getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File(WEB_ROOT + path);
        if (file.exists() && !file.isDirectory()) {
            try {
                String contentType = getContentType(path);
                response.setContentType(contentType);
                response.setBody(new String(Files.readAllBytes(file.toPath())));
            } catch (IOException e) {
                response.setStatus(500, "Internal Server Error");
                response.setBody("Error reading file");
            }
        } else {
            response.setStatus(404, "Not Found");
            response.setBody("404 Not Found");
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg")) return "image/jpeg";
        return "text/plain";
    }
}
