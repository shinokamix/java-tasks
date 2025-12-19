import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    
    private static final int PORT = 8080;
    
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            server.createContext("/login", new LoginHandler());
            server.createContext("/image", new ImageHandler());
            server.createContext("/game", new GameHandler());
            server.createContext("/delete", new DeleteHandler());
            
            server.setExecutor(null);
          
            server.start();
            System.out.println("Сервер на порту " + PORT);
            System.out.println("\n Доступные endpoints:\n");
            System.out.println("  1. GET  http://localhost:" + PORT + "/login");
            System.out.println("  2. GET  http://localhost:" + PORT + "/image");
            System.out.println("  3. POST http://localhost:" + PORT + "/game");
            System.out.println("  4. DELETE http://localhost:" + PORT + "/delete?path=<путь>");
        
            
        } catch (IOException e) {
            System.err.println("Ошибка запуска сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }
}