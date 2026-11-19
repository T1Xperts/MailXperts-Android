package au.com.t1xperts.mailxperts;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MX-QA-032: app-owned, offline-only, bounded and deliberately content-free telemetry.
 * No network requests. Not full device logcat. Backup is disabled for this app.
 */
final class DiagnosticLogger {
    private static final String FILE_NAME = "mx_diagnostics.txt";
    private static final String OLD_NAME = "mx_diagnostics.old.txt";
    private static final long MAX_FILE_SIZE = 96L * 1024L;
    private static final int MAX_DISPLAY_CHARS = 80 * 1024;
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final AtomicBoolean HANDLING = new AtomicBoolean(false);
    private static final Object FILE_LOCK = new Object();

    private DiagnosticLogger() {}

    static void install(Context context) {
        if (!INSTALLED.compareAndSet(false, true)) return;
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Context app = context.getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                if (HANDLING.compareAndSet(false, true)) {
                    append(app, "UNCAUGHT_EXCEPTION", "APPLICATION", error);
                }
            } catch (Throwable ignored) {
                // Diagnostics must never obscure the original failure.
            } finally {
                if (previous != null) previous.uncaughtException(thread, error);
            }
        });
        Thread worker = new Thread(() -> collectPreviousExits(app), "mx-exit-reasons");
        worker.setDaemon(true);
        worker.start();
    }

    static void breadcrumb(Context context, String event, String area) {
        append(context.getApplicationContext(), event, area, null);
    }

    static void nonFatal(Context context, String event, String area, Throwable error) {
        append(context.getApplicationContext(), event, area, error);
    }

    private static void append(Context context, String event, String area, Throwable error) {
        if (context == null) return;
        String line = timestamp() + " | " + DiagnosticSanitizer.code(event)
                + " | " + DiagnosticSanitizer.code(area)
                + " | " + DiagnosticSanitizer.exceptionClass(error)
                + " | " + DiagnosticSanitizer.appFrames(error)
                + " | app=" + BuildConfig.VERSION_NAME
                + " | android_api=" + Build.VERSION.SDK_INT + "\n";
        synchronized (FILE_LOCK) {
            try {
                File directory = context.getNoBackupFilesDir();
                File current = new File(directory, FILE_NAME);
                if (current.length() + line.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_SIZE) {
                    File prior = new File(directory, OLD_NAME);
                    if (prior.exists()) prior.delete();
                    if (current.exists()) current.renameTo(prior);
                }
                try (FileOutputStream out = new FileOutputStream(current, true)) {
                    out.write(line.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            } catch (Exception ignored) {
                // Must not break mail UI or the system's crash handler.
            }
        }
    }

    static String exportText(Context context) {
        synchronized (FILE_LOCK) {
            StringBuilder out = new StringBuilder("MailXperts local diagnostic report\n");
            out.append("Only app event codes, app stack-frame identifiers, app version and Android API level.\n");
            out.append("No email contents, recipient names, subject, credentials or attachments.\n");
            appendFrom(new File(context.getNoBackupFilesDir(), OLD_NAME), out);
            appendFrom(new File(context.getNoBackupFilesDir(), FILE_NAME), out);
            if (out.length() > MAX_DISPLAY_CHARS) {
                return "[Older entries omitted; local file limit applied]\n"
                        + out.substring(out.length() - MAX_DISPLAY_CHARS);
            }
            return out.toString();
        }
    }

    static void clear(Context context) {
        synchronized (FILE_LOCK) {
            new File(context.getNoBackupFilesDir(), FILE_NAME).delete();
            new File(context.getNoBackupFilesDir(), OLD_NAME).delete();
        }
    }

    private static void appendFrom(File file, StringBuilder out) {
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && out.length() < MAX_DISPLAY_CHARS * 3) {
                out.append(line).append('\n');
            }
        } catch (Exception ignored) {
            out.append("Local report file unavailable.\n");
        }
    }

    private static String timestamp() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private static void collectPreviousExits(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
        try {
            ActivityManager manager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null) return;
            List<ApplicationExitInfo> events = manager.getHistoricalProcessExitReasons(
                    context.getPackageName(), 0, 5);
            SharedPreferences prefs = context.getSharedPreferences("mx_diagnostic_meta", Context.MODE_PRIVATE);
            long last = prefs.getLong("last_exit_timestamp", 0L);
            long newest = last;
            for (int i = events.size() - 1; i >= 0; i--) {
                ApplicationExitInfo entry = events.get(i);
                if (entry.getTimestamp() <= last) continue;
                newest = Math.max(newest, entry.getTimestamp());
                int reason = entry.getReason();
                if (reason == ApplicationExitInfo.REASON_ANR) {
                    breadcrumb(context, "PREVIOUS_PROCESS_ANR", "APPLICATION");
                } else if (reason == ApplicationExitInfo.REASON_CRASH
                        || reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                    breadcrumb(context, "PREVIOUS_PROCESS_CRASH", "APPLICATION");
                }
            }
            if (newest > last) prefs.edit().putLong("last_exit_timestamp", newest).apply();
        } catch (Exception ignored) {
            // OEM variations must not prevent startup.
        }
    }
}
