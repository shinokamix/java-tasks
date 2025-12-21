import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Importer {

    // Имитация ответа от API (JSON массив в виде строки)
    private static final String MOCK_JSON_DATA = """
        [
          {
            "userId": 1,
            "id": 1,
            "title": "First incident report",
            "body": "Description of the first incident with some details."
          },
          {
            "userId": 1,
            "id": 2,
            "title": "Second incident",
            "body": "Everything is broken.\\nNeed urgent fix."
          },
          {
            "userId": 2,
            "id": 3,
            "title": "Network issue",
            "body": "Connection reset by peer at 10:00 AM"
          },
          {
            "userId": 3,
            "id": 4,
            "title": "Database lag",
            "body": "Slow queries detected on production."
          }
        ]
        """;

    private static final Path STORAGE_DIR = Paths.get("storage");
    private static final Path OUTPUT_FILE = STORAGE_DIR.resolve("incidents.ndjson");
    private static final Path LOG_FILE = Paths.get("import.log");
    private static final int DEFAULT_LIMIT = 2; // По умолчанию импортируем 2

    public static void main(String[] args) {
        int limit = DEFAULT_LIMIT;

        if (args.length > 0) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format. Using default: " + DEFAULT_LIMIT);
            }
        }

        System.out.println("Starting local import (Limit: " + limit + ")...");

        int successCount = 0;
        String errorMessage = "None";

        try {
            // 1. Создание папки
            if (!Files.exists(STORAGE_DIR)) {
                Files.createDirectories(STORAGE_DIR);
            }

            // 2. "Загрузка" (просто берем из константы)
            String rawJson = MOCK_JSON_DATA;

            // 3. Парсинг массива
            List<String> objects = splitJsonArray(rawJson);
            int processingLimit = Math.min(limit, objects.size());

            // 4. Запись в NDJSON
            try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_FILE, StandardCharsets.UTF_8, 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                
                for (int i = 0; i < processingLimit; i++) {
                    String incident = convertToNDJSON(objects.get(i));
                    writer.write(incident);
                    writer.newLine();
                    successCount++;
                }
            }
            
            System.out.println("Import finished. Processed: " + successCount);

        } catch (Exception e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
        } finally {
            writeLog(successCount, errorMessage);
        }
    }

    private static List<String> splitJsonArray(String json) {
        List<String> list = new ArrayList<>();
        String content = json.trim();
        if (content.startsWith("[") && content.endsWith("]")) {
            content = content.substring(1, content.length() - 1).trim();
        }

        // Разделяем объекты по комбинации "},"
        String[] parts = content.split("(?<=\\}),\\s*(?=\\{)");
        for (String p : parts) {
            if (!p.isBlank()) list.add(p.trim());
        }
        return list;
    }

    private static String convertToNDJSON(String raw) {
        String id = extract(raw, "id");
        String title = extract(raw, "title");
        String body = extract(raw, "body");
        String userId = extract(raw, "userId");
        String timestamp = Instant.now().toString();

        // Формируем одну строку JSON без переносов внутри
        return String.format(
            "{\"id\": %s, \"title\": \"%s\", \"body\": \"%s\", \"userId\": %s, \"importedAt\": \"%s\"}",
            id, escape(title), escape(body), userId, timestamp
        );
    }

    private static String extract(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return "null";

        start += pattern.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == ':')) {
            start++;
        }

        char firstChar = json.charAt(start);
        int end;
        if (firstChar == '"') {
            start++; // Пропускаем открывающую кавычку
            end = start;
            while (end < json.length() && !(json.charAt(end) == '"' && json.charAt(end-1) != '\\')) {
                end++;
            }
        } else {
            end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
        }
        return json.substring(start, end).trim();
    }

    private static String escape(String s) {
        return s.replace("\n", "\\n").replace("\"", "\\\"");
    }

    private static void writeLog(int count, String error) {
        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault()).format(Instant.now());
        String log = String.format("%s, %s, %d, %s", time, Thread.currentThread().getName(), count, error);
        
        try (BufferedWriter bw = Files.newBufferedWriter(LOG_FILE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            bw.write(log);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}