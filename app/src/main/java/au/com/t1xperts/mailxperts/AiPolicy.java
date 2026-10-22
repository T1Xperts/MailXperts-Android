package au.com.t1xperts.mailxperts;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class AiPolicy {
    private static final Set<String> TASKS = new HashSet<>(Arrays.asList(
            "summarize", "draft_reply", "ask", "search_plan",
            "priority", "extract_actions", "rewrite"));

    private AiPolicy() {}

    static boolean isAllowedTask(String task) {
        return task != null && TASKS.contains(task);
    }

    static String normaliseGateway(String raw) {
        String value = raw == null ? "" : raw.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        URI uri;
        try {
            uri = URI.create(value);
        } catch (Exception error) {
            throw new IllegalArgumentException("Enter a valid HTTPS AI gateway URL.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("MailXperts AI requires an HTTPS gateway.");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Do not put credentials in the gateway URL.");
        }
        // A gateway is an HTTPS origin or safe path-prefix, never a query or fragment.
        // Without this rule, appending /v1/mail/tasks may target the wrong endpoint.
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Remove query parameters and fragments from the gateway URL.");
        }
        String path = uri.getRawPath();
        if (path != null) {
            String lowered = path.toLowerCase(java.util.Locale.ROOT);
            if (lowered.contains("..") || lowered.contains("%2e")
                    || lowered.contains("%2f") || lowered.contains("%5c")
                    || path.contains("\\\\") || path.contains("//")) {
                throw new IllegalArgumentException("The gateway path must not contain traversal or encoded separators.");
            }
        }
        return value;
    }

    static String clamp(String value, int maxChars) {
        String safe = value == null ? "" : value;
        int max = Math.max(0, maxChars);
        return safe.length() <= max ? safe : safe.substring(0, max);
    }
}
