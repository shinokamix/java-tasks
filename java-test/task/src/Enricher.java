import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Enricher {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String POISON_PILL = "__POISON__";

    public static void main(String[] args) {
        Args a = Args.parse(args);

        try {
            Files.createDirectories(Paths.get("storage"));
            Files.createDirectories(Paths.get("logs"));
        } catch (IOException e) {
            System.err.println("Failed to create directories: " + e.getMessage());
            return;
        }

        Path in = Paths.get("storage", "incidents.ndjson");
        Path out = Paths.get("storage", "incidents_enriched.ndjson");

        if (!Files.exists(in)) {
            System.err.println("Input file not found: " + in);
            log("enrich.log", Instant.now(), "main", "ERROR: input missing: " + in);
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(a.threads);
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(10_000);

        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        Thread writerThread = new Thread(() -> writerLoop(queue, out), "writer");
        writerThread.start();

        Instant started = Instant.now();
        log("enrich.log", started, "main", "START threads=" + a.threads);

        try (BufferedReader r = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                final String incidentLine = line;
                pool.submit(() -> enrichOne(a.baseUrl, incidentLine, queue, ok, fail));
            }
        } catch (IOException e) {
            log("enrich.log", Instant.now(), "main", "FATAL read: " + e.getMessage());
            System.err.println("Read error: " + e.getMessage());
        }

        pool.shutdown();
        try {
            boolean terminated = pool.awaitTermination(10, TimeUnit.MINUTES);
            if (!terminated) {
                log("enrich.log", Instant.now(), "main", "WARN: pool not terminated in time; forcing shutdownNow");
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }

        // stop writer
        try {
            queue.put(POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            writerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Instant finished = Instant.now();
        String summary = "DONE ok=" + ok.get() + " fail=" + fail.get() +
                " started=" + started + " finished=" + finished;
        log("enrich.log", Instant.now(), "main", summary);
        System.out.println(summary);
    }

    private static void enrichOne(
            String baseUrl,
            String incidentNdjsonLine,
            BlockingQueue<String> queue,
            AtomicInteger ok,
            AtomicInteger fail
    ) {
        String thread = Thread.currentThread().getName();
        Instant ts = Instant.now();

        try {
            Incident inc = Incident.fromNdjson(incidentNdjsonLine);
            if (inc.id == null) throw new IllegalArgumentException("incident id missing");

            String url = baseUrl + "/comments?postId=" + inc.id;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                fail.incrementAndGet();
                log("enrich.log", ts, thread, "ERROR id=" + inc.id + " HTTP " + resp.statusCode());
                return;
            }

            List<String> comments = JsonMini.splitTopLevelObjects(resp.body()); // expects array
            int commentsCount = comments.size();

            HashSet<String> emails = new HashSet<>();
            for (String c : comments) {
                String email = JsonMini.getStringOrNull(c, "email");
                if (email != null) emails.add(email.toLowerCase(Locale.ROOT));
            }

            inc.commentsCount = commentsCount;
            inc.uniqueEmailsCount = emails.size();
            inc.enrichedAt = Instant.now().toString();

            String outLine = inc.toJson();
            queue.put(outLine);

            ok.incrementAndGet();
            log("enrich.log", Instant.now(), thread,
                    "OK id=" + inc.id + " commentsCount=" + commentsCount + " uniqueEmailsCount=" + emails.size());

        } catch (Exception e) {
            fail.incrementAndGet();
            log("enrich.log", Instant.now(), thread, "ERROR: " + e.getMessage());
        }
    }

    private static void writerLoop(BlockingQueue<String> queue, Path out) {
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            while (true) {
                String line = queue.take();
                if (POISON_PILL.equals(line)) break;
                w.write(line);
                w.newLine();
            }

        } catch (Exception e) {
            log("enrich.log", Instant.now(), Thread.currentThread().getName(),
                    "FATAL writer: " + e.getMessage());
        }
    }

    private static void log(String fileName, Instant ts, String threadName, String message) {
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

    static class Args {
        String baseUrl = "https://jsonplaceholder.typicode.com";
        int threads = 6;

        static Args parse(String[] args) {
            Args a = new Args();
            for (int i = 0; i < args.length; i++) {
                String k = args[i];
                if ("--baseUrl".equals(k) && i + 1 < args.length) a.baseUrl = args[++i];
                else if ("--threads".equals(k) && i + 1 < args.length) a.threads = Integer.parseInt(args[++i]);
            }
            if (a.threads < 1) a.threads = 1;
            return a;
        }
    }
}
