package com.fincore.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String path;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> queryParams = new HashMap<>();
    private StringBuilder body = new StringBuilder();

    public HttpRequest(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null || line.isEmpty()) return;

        // Parse Request Line
        String[] parts = line.split(" ");
        this.method = parts[0];
        String fullPath = parts[1];
        
        // Parse Query Params
        if (fullPath.contains("?")) {
            String[] pathParts = fullPath.split("\\?");
            this.path = pathParts[0];
            String[] params = pathParts[1].split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2) {
                    queryParams.put(kv[0], kv[1]);
                }
            }
        } else {
            this.path = fullPath;
        }

        // Parse Headers
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            String[] headerParts = line.split(": ");
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }

        // Parse Body (if Content-Length exists)
        if (headers.containsKey("Content-Length")) {
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            char[] buffer = new char[contentLength];
            reader.read(buffer, 0, contentLength);
            body.append(buffer);
        }
    }

    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getBody() { return body.toString(); }
    public String getQueryParam(String key) { return queryParams.get(key); }
}
