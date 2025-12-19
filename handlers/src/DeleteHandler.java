import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;


public class DeleteHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = TokenManager.extractToken(authHeader);
        
        if (!TokenManager.isValidToken(token)) {
            sendResponse(exchange, 403, "Forbidden: Invalid or missing token");
            return;
        }
        
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();
        
        if (query == null || !query.startsWith("path=")) {
            sendResponse(exchange, 400, "Bad Request: Missing 'path' query parameter. Use: /delete?path=/absolute/path");
            return;
        }
        
        String path = query.substring(5); 
        path = java.net.URLDecoder.decode(path, "UTF-8"); 
        
        if (path.isEmpty()) {
            sendResponse(exchange, 400, "Bad Request: Path cannot be empty");
            return;
        }
        
        File fileToDelete = new File(path);
        
        if (!fileToDelete.exists()) {
            sendResponse(exchange, 404, "Not Found: File or directory does not exist: " + path);
            return;
        }
        
        try {
            boolean deleted = deleteRecursively(fileToDelete);
            
            if (deleted) {
                System.out.println("Deleted: " + path);
                sendResponse(exchange, 200, "Successfully deleted: " + path);
            } else {
                sendResponse(exchange, 500, "Internal Server Error: Failed to delete: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error deleting file: " + e.getMessage());
            sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
    

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}