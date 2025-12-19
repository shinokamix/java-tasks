import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;


public class ImageHandler implements HttpHandler {
    
    private static final String IMAGE_FILENAME = "pic.jpg";
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }
        
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = TokenManager.extractToken(authHeader);
        
        if (!TokenManager.isValidToken(token)) {
            sendResponse(exchange, 403, "Forbidden: Invalid or missing token", "text/plain");
            return;
        }
        
        File imageFile = new File(IMAGE_FILENAME);
        
        if (!imageFile.exists()) {
            sendResponse(exchange, 404, "Image file 'pic.jpg' not found", "text/plain");
            return;
        }
        
        try {
            
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            
            exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, imageBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(imageBytes);
            }
            
        } catch (IOException e) {
            sendResponse(exchange, 500, "Error reading image file", "text/plain");
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}