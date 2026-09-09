package au.com.t1xperts.mailxperts;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Persistable reference to an outgoing attachment copied into MailXperts private storage. */
final class AttachmentRef {
    final String path;
    final String name;
    final String mimeType;
    final long size;

    AttachmentRef(String path, String name, String mimeType, long size) {
        this.path = path == null ? "" : path;
        this.name = name == null || name.trim().isEmpty() ? "attachment" : name;
        this.mimeType = mimeType == null || mimeType.trim().isEmpty()
                ? "application/octet-stream" : mimeType;
        this.size = Math.max(0L, size);
    }

    File file() { return new File(path); }

    boolean exists() { return !path.isEmpty() && file().isFile(); }

    static String encode(List<AttachmentRef> attachments) {
        if (attachments == null || attachments.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (AttachmentRef attachment : attachments) {
            if (attachment == null || attachment.path.isEmpty()) continue;
            if (out.length() > 0) out.append('\n');
            out.append(token(attachment.path)).append('|')
                    .append(token(attachment.name)).append('|')
                    .append(token(attachment.mimeType)).append('|')
                    .append(attachment.size);
        }
        return out.toString();
    }

    static ArrayList<AttachmentRef> decode(String encoded) {
        ArrayList<AttachmentRef> out = new ArrayList<>();
        if (encoded == null || encoded.trim().isEmpty()) return out;
        String[] rows = encoded.split("\\n");
        for (String row : rows) {
            String[] parts = row.split("\\|", -1);
            if (parts.length != 4) continue;
            try {
                long size = Long.parseLong(parts[3]);
                AttachmentRef attachment = new AttachmentRef(
                        untoken(parts[0]), untoken(parts[1]), untoken(parts[2]), size);
                if (!attachment.path.isEmpty()) out.add(attachment);
            } catch (RuntimeException ignored) {
                // Ignore one corrupt legacy row rather than losing the whole draft.
            }
        }
        return out;
    }

    static void deleteFiles(List<AttachmentRef> attachments) {
        if (attachments == null) return;
        for (AttachmentRef attachment : attachments) {
            if (attachment == null || attachment.path.isEmpty()) continue;
            try {
                File file = attachment.file();
                if (file.isFile()) file.delete();
            } catch (RuntimeException ignored) {}
        }
    }

    private static String token(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String untoken(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
