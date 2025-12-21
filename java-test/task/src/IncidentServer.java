import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class IncidentServer {

    private final ConcurrentHashMap<Integer, Incident> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Integer>> titleIndex = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    private final Object localFileLock = new Object();
    private final Path localFile = Paths.get("storage", "local_incidents.ndjson");

    public static void main(String[] args) throws Exception {
        int port = 8080;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) port = Integer.parseInt(args[++i]);
        }

        Files.createDirectories(Paths.get("storage"));
        Files.createDirectories(Paths.get("logs"));

        IncidentServer app = new IncidentServer();
        app.loadOnStart();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/incidents", app::handleIncidents);
        server.createContext("/incidents/search", app::handleSearch);

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        app.log("server.log", Instant.now(), "main",
                "START port=" + port + " loaded=" + app.store.size() + " nextId=" + app.nextId.get());

        System.out.println("Server started on http://localhost:" + port);
    }

    private void loadOnStart() {
        loadNdjsonIfExists(Paths.get("storage", "incidents_enriched.ndjson"));
        loadNdjsonIfExists(localFile);

        int max = store.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        nextId.set(max + 1);

        rebuildIndex();
    }

    private void loadNdjsonIfExists(Path p) {
        if (!Files.exists(p)) return;
        try (BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    Incident inc = Incident.fromNdjson(line);
                    if (inc.id != null) store.put(inc.id, inc);
                } catch (Exception ignored) { }
            }
        } catch (IOException ignored) { }
    }

    private void rebuildIndex() {
        titleIndex.clear();
        for (Incident inc : store.values()) {
            indexIncidentTitle(inc);
        }
    }

    private void indexIncidentTitle(Incident inc) {
        if (inc.id == null) return;
        String t = inc.title == null ? "" : inc.title.toLowerCase(Locale.ROOT);
        for (String w : t.split("[^\\p{L}\\p{Nd}]+")) {
            if (w.isBlank()) continue;
            titleIndex.compute(w, (k, set) -> {
                if (set == null) set = ConcurrentHashMap.newKeySet();
                set.add(inc.id);
                return set;
            });
        }
    }

    private void handleIncidents(HttpExchange ex) throws IOException {
        long t0 = System.nanoTime();
        int code = 500;

        try {
            String method = ex.getRequestMethod();
            URI uri = ex.getRequestURI();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> q = parseQuery(uri.getRawQuery());
                String idStr = q.get("id");
                if (idStr == null) {
                    code = 400;
                    writeJson(ex, code, "{\"error\":\"missing id\"}");
                    return;
                }

                Integer id;
                try { id = Integer.parseInt(idStr); }
                catch (NumberFormatException nfe) {
                    code = 400;
                    writeJson(ex, code, "{\"error\":\"bad id\"}");
                    return;
                }

                Incident inc = store.get(id);
                if (inc == null) {
                    code = 404;
                    writeJson(ex, code, "{\"error\":\"not found\"}");
                    return;
                }

                code = 200;
                writeJson(ex, code, inc.toJson());
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                String body = readAll(ex.getRequestBody());
                String title = JsonMini.getStringOrNull(body, "title");
                String text = JsonMini.getStringOrNull(body, "body");

                if (title == null || text == null || title.isBlank() || text.isBlank()) {
                    code = 400;
                    writeJson(ex, code, "{\"error\":\"missing title/body\"}");
                    return;
                }

                int id = nextId.getAndIncrement();
                Incident inc = new Incident();
                inc.id = id;
                inc.title = title;
                inc.body = text;
                inc.createdAt = Instant.now().toString();
                inc.commentsCount = 0;
                inc.uniqueEmailsCount = 0;

                store.put(id, inc);
                indexIncidentTitle(inc);

                // append local storage safely
                String line = inc.toJson();
                synchronized (localFileLock) {
                    try (BufferedWriter w = Files.newBufferedWriter(localFile, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                        w.write(line);
                        w.newLine();
                    }
                }

                code = 201;
                writeJson(ex, code, inc.toJson());
                return;
            }

            code = 405;
            writeJson(ex, code, "{\"error\":\"method not allowed\"}");

        } catch (Exception e) {
            code = 500;
            writeJson(ex, code, "{\"error\":\"internal\"}");
        } finally {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            log("server.log", Instant.now(), Thread.currentThread().getName(),
                    ex.getRequestMethod() + " " + ex.getRequestURI().getPath() +
                            " -> " + code + " in " + ms + "ms");
        }
    }

    private void handleSearch(HttpExchange ex) throws IOException {
        long t0 = System.nanoTime();
        int code = 500;

        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                code = 405;
                writeJson(ex, code, "{\"error\":\"method not allowed\"}");
                return;
            }

            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
            String query = q.get("q");
            if (query == null || query.isBlank()) {
                code = 400;
                writeJson(ex, code, "{\"error\":\"missing q\"}");
                return;
            }

            String qLower = query.toLowerCase(Locale.ROOT).trim();

            // быстрый путь по индексу: если один “токен”
            Set<Integer> candidates = null;
            String[] tokens = qLower.split("[^\\p{L}\\p{Nd}]+");
            List<String> toks = new ArrayList<>();
            for (String t : tokens) if (!t.isBlank()) toks.add(t);

            if (toks.size() == 1) {
                candidates = titleIndex.getOrDefault(toks.get(0), Collections.emptySet());
            }

            List<String> results = new ArrayList<>();
            if (candidates != null) {
                for (Integer id : candidates) {
                    Incident inc = store.get(id);
                    if (inc != null) results.add(inc.toJson());
                }
            } else {
                // fallback: substring search по title/body
                for (Incident inc : store.values()) {
                    String t = (inc.title == null ? "" : inc.title).toLowerCase(Locale.ROOT);
                    String b = (inc.body == null ? "" : inc.body).toLowerCase(Locale.ROOT);
                    if (t.contains(qLower) || b.contains(qLower)) {
                        results.add(inc.toJson());
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < results.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(results.get(i));
            }
            sb.append("]");

            code = 200;
            writeJson(ex, code, sb.toString());

        } catch (Exception e) {
            code = 500;
            writeJson(ex, code, "{\"error\":\"internal\"}");
        } finally {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            log("server.log", Instant.now(), Thread.currentThread().getName(),
                    ex.getRequestMethod() + " " + ex.getRequestURI().getPath() +
                            " -> " + code + " in " + ms + "ms");
        }
    }

    private static void writeJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toString(StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        String[] parts = raw.split("&");
        for (String p : parts) {
            int i = p.indexOf('=');
            if (i < 0) continue;
            String k = urlDecode(p.substring(0, i));
            String v = urlDecode(p.substring(i + 1));
            m.put(k, v);
        }
        return m;
    }

    private static String urlDecode(String s) {
        // минимально достаточно для query; без URLDecoder чтобы не тянуть нюансы '+' — но сделаем аккуратно:
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private void log(String fileName, Instant ts, String threadName, String message) {
        Path p = Paths.get("logs", fileName);
        String line = DateTimeFormatter.ISO_INSTANT.format(ts) +
                ", " + threadName +
                ", " + message;
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (IOException ignored) { }
    }
}
