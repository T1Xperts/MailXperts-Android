package au.com.t1xperts.mailxperts;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * MX-QA-031: never put the full quoted HTML thread into an Activity Intent.
 * Payloads stay in this app's private no-backup directory and expire after 24h.
 */
final class ReplyPayloadStore {
    static final String EXTRA_TOKEN = "mx_reply_quote_token";
    static final int MAX_QUOTE_BYTES = 2_000_000;
    private static final long MAX_AGE_MS = 24L * 60 * 60 * 1000;

    private ReplyPayloadStore() {}

    static String save(Context context, String html) throws IOException {
        return saveIn(new File(context.getNoBackupFilesDir(), "reply_quotes"), html);
    }

    static String load(Context context, String token) throws IOException {
        return loadFrom(new File(context.getNoBackupFilesDir(), "reply_quotes"), token);
    }

    /** Package-private filesystem operations permit real JVM tests without Android mocks. */
    static String saveIn(File directory, String html) throws IOException {
        if (html == null) throw new IOException("Quote missing");
        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        if (data.length > MAX_QUOTE_BYTES) throw new IOException("Quote exceeds safe editor limit");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Quote storage unavailable");
        cleanup(directory);
        String token = UUID.randomUUID().toString();
        File temporary = new File(directory, token + ".tmp");
        File destination = file(directory, token);
        try {
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(data);
                output.getFD().sync();
            }
            if (!temporary.renameTo(destination)) throw new IOException("Could not finalise quote");
            return token;
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    static String loadFrom(File directory, String token) throws IOException {
        if (!validToken(token)) throw new IOException("Invalid quote reference");
        File source = file(directory, token);
        if (!source.isFile() || source.length() > MAX_QUOTE_BYTES
                || source.length() < 0 || System.currentTimeMillis() - source.lastModified() > MAX_AGE_MS) {
            throw new IOException("Quoted message unavailable or expired");
        }
        try (FileInputStream input = new FileInputStream(source)) {
            byte[] buffer = new byte[(int) source.length()];
            int used = 0, n;
            while (used < buffer.length && (n = input.read(buffer, used, buffer.length - used)) > 0) {
                used += n;
            }
            if (used != buffer.length) throw new IOException("Incomplete quoted message");
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }

    private static boolean validToken(String token) {
        return token != null && token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    private static File file(File directory, String token) {
        return new File(directory, token + ".html");
    }

    private static void cleanup(File directory) {
        File[] candidates = directory.listFiles();
        if (candidates == null) return;
        long now = System.currentTimeMillis();
        for (File candidate : candidates) {
            if (candidate.isFile() && now - candidate.lastModified() > MAX_AGE_MS) {
                candidate.delete();
            }
        }
    }
}
