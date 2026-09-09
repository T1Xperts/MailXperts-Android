package au.com.t1xperts.mailxperts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

/** Imports user-selected documents into app-private storage so Draft/Outbox references survive. */
final class AttachmentStorage {
    static final long MAX_ATTACHMENT_BYTES = 50L * 1024L * 1024L;

    private AttachmentStorage() {}

    static AttachmentRef importUri(Context context, Uri uri) throws IOException {
        if (context == null || uri == null) throw new IOException("Attachment is unavailable.");
        ContentResolver resolver = context.getContentResolver();
        String name = "attachment";
        long reportedSize = -1L;
        try (Cursor cursor = resolver.query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex);
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) reportedSize = cursor.getLong(sizeIndex);
            }
        } catch (RuntimeException ignored) {}

        if (reportedSize > MAX_ATTACHMENT_BYTES) {
            throw new IOException("Attachment is larger than the 50 MB MailXperts safety limit.");
        }

        String mime = resolver.getType(uri);
        if (mime == null || mime.trim().isEmpty()) mime = "application/octet-stream";
        File directory = new File(context.getFilesDir(), "attachments");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create attachment storage.");
        }
        File target = new File(directory, UUID.randomUUID().toString() + "_" + safeName(name));
        long copied = 0L;
        try (InputStream input = resolver.openInputStream(uri);
             FileOutputStream output = new FileOutputStream(target)) {
            if (input == null) throw new IOException("Could not open selected attachment.");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                copied += read;
                if (copied > MAX_ATTACHMENT_BYTES) {
                    throw new IOException("Attachment is larger than the 50 MB MailXperts safety limit.");
                }
                output.write(buffer, 0, read);
            }
            output.flush();
        } catch (IOException error) {
            target.delete();
            throw error;
        }
        return new AttachmentRef(target.getAbsolutePath(), name, mime, copied);
    }

    static String displaySize(long bytes) {
        long safe = Math.max(0L, bytes);
        if (safe < 1024L) return safe + " B";
        double kb = safe / 1024d;
        if (kb < 1024d) return String.format(Locale.getDefault(), "%.1f KB", kb);
        double mb = kb / 1024d;
        return String.format(Locale.getDefault(), "%.1f MB", mb);
    }

    static String safeName(String value) {
        String name = value == null ? "attachment" : value.trim();
        if (name.isEmpty()) name = "attachment";
        name = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        if (name.length() > 120) name = name.substring(name.length() - 120);
        return name;
    }
}
