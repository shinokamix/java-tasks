import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class TokenManager {
    private static final Set<String> validTokens = new HashSet<>();
    
  
    public static String generateToken() {
        String token = UUID.randomUUID().toString();
        validTokens.add(token);
        System.out.println("Новый токен создан: " + token);
        return token;
    }
    
   
    public static boolean isValidToken(String token) {
        return token != null && validTokens.contains(token);
    }
    
    public static String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
    
    
    public static int getTokenCount() {
        return validTokens.size();
    }
}