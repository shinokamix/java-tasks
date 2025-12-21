import java.util.LinkedHashMap;
import java.util.Map;

public class Incident {
    // required for imported
    Integer id;
    String title;
    String body;
    Integer userId;
    String importedAt;

    // enriched
    Integer commentsCount;
    Integer uniqueEmailsCount;
    String enrichedAt;

    // local created
    String createdAt;

    public static Incident fromNdjson(String line) {
        // line is a JSON object
        Incident inc = new Incident();
        inc.id = JsonMini.getIntOrNull(line, "id");
        inc.userId = JsonMini.getIntOrNull(line, "userId");
        inc.title = JsonMini.getStringOrNull(line, "title");
        inc.body = JsonMini.getStringOrNull(line, "body");
        inc.importedAt = JsonMini.getStringOrNull(line, "importedAt");
        inc.commentsCount = JsonMini.getIntOrNull(line, "commentsCount");
        inc.uniqueEmailsCount = JsonMini.getIntOrNull(line, "uniqueEmailsCount");
        inc.enrichedAt = JsonMini.getStringOrNull(line, "enrichedAt");
        inc.createdAt = JsonMini.getStringOrNull(line, "createdAt");
        return inc;
    }

    public String toJson() {
        // keep stable order (handy for diff/logs)
        Map<String, Object> m = new LinkedHashMap<>();

        if (id != null) m.put("id", id);
        if (title != null) m.put("title", title);
        if (body != null) m.put("body", body);
        if (userId != null) m.put("userId", userId);
        if (importedAt != null) m.put("importedAt", importedAt);

        if (commentsCount != null) m.put("commentsCount", commentsCount);
        if (uniqueEmailsCount != null) m.put("uniqueEmailsCount", uniqueEmailsCount);
        if (enrichedAt != null) m.put("enrichedAt", enrichedAt);

        if (createdAt != null) m.put("createdAt", createdAt);

        return JsonMini.obj(m);
    }
}
