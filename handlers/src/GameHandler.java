import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;


public class GameHandler implements HttpHandler {
    
    private static final String[] CHOICES = {"rock", "paper", "scissors"};
    private static final Random random = new Random();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = TokenManager.extractToken(authHeader);
        
        if (!TokenManager.isValidToken(token)) {
            sendJsonResponse(exchange, 403, "{\"error\":\"Forbidden: Invalid or missing token\"}");
            return;
        }
        
        String requestBody = readRequestBody(exchange);
        
        String playerChoice = parsePlayerChoice(requestBody);
        
        if (playerChoice == null || !isValidChoice(playerChoice)) {
            sendJsonResponse(exchange, 400, 
                "{\"error\":\"Invalid choice. Use: rock, paper, or scissors\"}");
            return;
        }
        
        String serverChoice = CHOICES[random.nextInt(CHOICES.length)];
       
        String result = determineWinner(playerChoice, serverChoice);
        
        String jsonResponse = String.format(
            "{\"playerChoice\":\"%s\",\"serverChoice\":\"%s\",\"result\":\"%s\"}",
            playerChoice, serverChoice, result
        );
        
        System.out.println("Game: Player=" + playerChoice + ", Server=" + serverChoice + ", Result=" + result);
        sendJsonResponse(exchange, 200, jsonResponse);
    }
    
   
    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
        }
        return body.toString();
    }
    
  
    private String parsePlayerChoice(String json) {
        
        String searchFor = "\"choice\"";
        int choiceIndex = json.indexOf(searchFor);
        if (choiceIndex == -1) {
            return null;
        }
        
        int colonIndex = json.indexOf(":", choiceIndex);
        if (colonIndex == -1) {
            return null;
        }
        
        int startQuote = json.indexOf("\"", colonIndex);
        if (startQuote == -1) {
            return null;
        }
        
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) {
            return null;
        }
        
        return json.substring(startQuote + 1, endQuote).toLowerCase();
    }
    
    
    private boolean isValidChoice(String choice) {
        for (String validChoice : CHOICES) {
            if (validChoice.equals(choice)) {
                return true;
            }
        }
        return false;
    }
    
   
    private String determineWinner(String playerChoice, String serverChoice) {
        if (playerChoice.equals(serverChoice)) {
            
            return "draw";
        }
        
        // Правила игры
        if ((playerChoice.equals("rock") && serverChoice.equals("scissors")) ||
            (playerChoice.equals("scissors") && serverChoice.equals("paper")) ||
            (playerChoice.equals("paper") && serverChoice.equals("rock"))) {
            return "win";
        }
        
        return "lose";
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}