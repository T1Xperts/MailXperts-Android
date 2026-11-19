package au.com.t1xperts.mailxperts;

/**
 * Data-minimised diagnostic formatting. Never copy exception messages, user text,
 * email headers, HTML, URIs, account identities or arbitrary logcat into reports.
 */
final class DiagnosticSanitizer {
    private DiagnosticSanitizer() {}

    static String code(String value) {
        return value != null && value.matches("[A-Z][A-Z0-9_]{0,39}") ? value : "OTHER";
    }

    static String exceptionClass(Throwable error) {
        if (error == null) return "NONE";
        String name = error.getClass().getSimpleName();
        return name.matches("[A-Za-z][A-Za-z0-9_$]{0,79}") ? name : "UNKNOWN";
    }

    static String appFrames(Throwable error) {
        if (error == null) return "NONE";
        StringBuilder frames = new StringBuilder();
        int count = 0;
        for (StackTraceElement element : error.getStackTrace()) {
            String name = element.getClassName();
            String method = element.getMethodName();
            if (!name.startsWith("au.com.t1xperts.mailxperts.")
                    || !name.matches("[A-Za-z0-9_.$]+")
                    || !method.matches("[A-Za-z0-9_$<>]+")) continue;
            if (count++ >= 8) break;
            if (frames.length() > 0) frames.append(";");
            frames.append(name).append("#").append(method).append(":")
                    .append(Math.max(0, element.getLineNumber()));
        }
        return frames.length() == 0 ? "NO_APP_FRAMES" : frames.toString();
    }
}
