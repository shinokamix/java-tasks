import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.net.ProxySelector;
import java.time.Duration;

public class Importer {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)      // <-- важно
    .connectTimeout(Duration.ofSeconds(10))
    .proxy(ProxySelector.getDefault())         // <-- не мешает, часто помогает
    .build();
    public static void main(String[] args) {
        Args a = Args.parse(args);

        int imported = 0;
        int errors = 0;
        Instant started = Instant.now();

        try {
            Files.createDirectories(Paths.get("storage"));
            Files.createDirectories(Paths.get("logs"));
        } catch (IOException e) {
            System.err.println("Failed to create directories: " + e.getMessage());
            return;
        }

        String url = a.baseUrl + "/posts?_limit=" + a.limit;

        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "JavaHttpClient")
                .GET()
                .build();


            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                errors++;
                log("import.log", started, Thread.currentThread().getName(), imported,
                        "HTTP " + resp.statusCode() + " for " + url);
                System.err.println("HTTP error: " + resp.statusCode());
                return;
            }

            String body = resp.body();
            List<String> objs = JsonMini.splitTopLevelObjects(body); // expects JSON array

            Path out = Paths.get("storage", "incidents.ndjson");
            try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                for (String obj : objs) {
                    try {
                        int id = JsonMini.getInt(obj, "id");
                        int userId = JsonMini.getInt(obj, "userId");
                        String title = JsonMini.getString(obj, "title");
                        String postBody = JsonMini.getString(obj, "body");

                        String importedAt = Instant.now().toString();
                        Incident inc = new Incident();
                        inc.id = id;
                        inc.userId = userId;
                        inc.title = title;
                        inc.body = postBody;
                        inc.importedAt = importedAt;

                        w.write(inc.toJson());
                        w.newLine();
                        imported++;
                    } catch (Exception ex) {
                        errors++;
                        log("import.log", Instant.now(), Thread.currentThread().getName(), imported,
                                "Parse/write error: " + ex.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            errors++;
            log("import.log", Instant.now(), Thread.currentThread().getName(), imported,
                    "Fatal: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.err.println("Fatal: " + e.getMessage());
        }

        Instant finished = Instant.now();
        String summary = "done; imported=" + imported + "; errors=" + errors +
                "; started=" + started + "; finished=" + finished;
        log("import.log", Instant.now(), "main", imported, summary);
        System.out.println(summary);
    }

    private static void log(String fileName, Instant ts, String threadName, int imported, String message) {
        Path p = Paths.get("logs", fileName);
        String line = DateTimeFormatter.ISO_INSTANT.format(ts) +
                ", " + threadName +
                ", imported=" + imported +
                ", " + message;
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (IOException ignored) { }
    }

    static class Args {
        String baseUrl = "https://jsonplaceholder.typicode.com";
        int limit = 10;

        static Args parse(String[] args) {
            Args a = new Args();
            for (int i = 0; i < args.length; i++) {
                String k = args[i];
                if ("--baseUrl".equals(k) && i + 1 < args.length) a.baseUrl = args[++i];
                else if ("--limit".equals(k) && i + 1 < args.length) a.limit = Integer.parseInt(args[++i]);
            }
            if (a.limit < 0) a.limit = 0;
            return a;
        }
    }
}
