package com.fincore.server;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new HashMap<>();
    private String body = "";
    private OutputStream out;

    public HttpResponse(OutputStream out) {
        this.out = out;
        this.headers.put("Content-Type", "text/html");
        this.headers.put("Access-Control-Allow-Origin", "*"); // CORS for dev
    }

    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    public void setContentType(String type) {
        headers.put("Content-Type", type);
    }

    public void setBody(String body) {
        this.body = body;
        headers.put("Content-Length", String.valueOf(body.getBytes().length));
    }

    public void send() {
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                writer.println(header.getKey() + ": " + header.getValue());
            }
            writer.println(); // Empty line between headers and body
            writer.print(body);
            writer.flush();
        }
    }
}
