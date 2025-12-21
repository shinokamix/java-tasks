import java.util.*;

public class JsonMini {

    // ---- Public helpers ----

    // Split top-level JSON objects from an array string like: [ {...}, {...} ]
    public static List<String> splitTopLevelObjects(String jsonArray) {
        String s = jsonArray.trim();
        if (!s.startsWith("[") || !s.endsWith("]")) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        s = s.substring(1, s.length() - 1).trim();
        List<String> out = new ArrayList<>();
        if (s.isEmpty()) return out;

        int depth = 0;
        boolean inStr = false;
        boolean esc = false;
        int start = -1;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (inStr) {
                if (esc) esc = false;
                else if (c == '\\') esc = true;
                else if (c == '"') inStr = false;
                continue;
            } else {
                if (c == '"') { inStr = true; continue; }
            }

            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    out.add(s.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return out;
    }

    public static int getInt(String obj, String key) {
        Integer v = getIntOrNull(obj, key);
        if (v == null) throw new IllegalArgumentException("Missing int field: " + key);
        return v;
    }

    public static Integer getIntOrNull(String obj, String key) {
        String raw = findRawValue(obj, key);
        if (raw == null) return null;
        raw = raw.trim();
        // number only (no floats needed here)
        int i = 0;
        if (raw.startsWith("-")) i++;
        while (i < raw.length() && Character.isDigit(raw.charAt(i))) i++;
        String num = raw.substring(0, i);
        if (num.isEmpty() || "-".equals(num)) return null;
        return Integer.parseInt(num);
    }

    public static String getString(String obj, String key) {
        String v = getStringOrNull(obj, key);
        if (v == null) throw new IllegalArgumentException("Missing string field: " + key);
        return v;
    }

    public static String getStringOrNull(String obj, String key) {
        String raw = findRawValue(obj, key);
        if (raw == null) return null;
        raw = raw.trim();
        if (!raw.startsWith("\"")) return null;
        return parseJsonString(raw);
    }

    // Build JSON object from map (strings are escaped)
    public static String obj(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(e.getKey())).append("\":");
            sb.append(val(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    // ---- Internal parsing ----

    private static String val(Object o) {
        if (o == null) return "null";
        if (o instanceof Number || o instanceof Boolean) return String.valueOf(o);
        return "\"" + escape(String.valueOf(o)) + "\"";
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    // Finds raw JSON value substring starting at the first char of value.
    // Works for primitive/string values, not for nested objects/arrays extraction (not needed here).
    private static String findRawValue(String obj, String key) {
    // Упрощение под формат типа JSONPlaceholder: ключи уникальны, в значениях такой подстроки почти нет.
        String needle = "\"" + key + "\"";
        int k = obj.indexOf(needle);
        if (k < 0) return null;

        int colon = obj.indexOf(':', k + needle.length());
        if (colon < 0) return null;

        int i = colon + 1;
        while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) i++;

        return obj.substring(i); // дальше getInt/getString уже отрежут токен
    }


    private static int findStringEnd(String s, int quotePos) {
        int i = quotePos + 1;
        boolean esc = false;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (esc) esc = false;
            else if (c == '\\') esc = true;
            else if (c == '"') return i;
            i++;
        }
        return -1;
    }

    private static String parseJsonString(String rawStartingWithQuote) {
        // rawStartingWithQuote begins with '"', may contain more tokens after the string.
        int end = findStringEnd(rawStartingWithQuote, 0);
        if (end < 0) throw new IllegalArgumentException("Bad JSON string");
        String token = rawStartingWithQuote.substring(0, end + 1);
        return unescapeJsonString(token);
    }

    private static String unescapeJsonString(String tokenWithQuotes) {
        // tokenWithQuotes: "...."
        if (tokenWithQuotes.length() < 2) return "";
        String s = tokenWithQuotes.substring(1, tokenWithQuotes.length() - 1);
        StringBuilder sb = new StringBuilder(s.length());
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!esc) {
                if (c == '\\') esc = true;
                else sb.append(c);
            } else {
                esc = false;
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('"');
                    default -> sb.append(c); // minimal
                }
            }
        }
        return sb.toString();
    }
}
