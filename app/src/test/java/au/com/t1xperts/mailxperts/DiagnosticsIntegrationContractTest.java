package au.com.t1xperts.mailxperts;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class DiagnosticsIntegrationContractTest {
    private String read(String relative) throws Exception {
        java.nio.file.Path file = Paths.get("app/src/main/" + relative);
        if (!Files.exists(file)) file = Paths.get("src/main/" + relative);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test public void userCanExportWithoutAnyBackgroundSending() throws Exception {
        String ui = read("java/au/com/t1xperts/mailxperts/DiagnosticsActivity.java");
        String log = read("java/au/com/t1xperts/mailxperts/DiagnosticLogger.java");
        String manifest = read("AndroidManifest.xml");
        assertTrue(ui.contains("ACTION_CREATE_DOCUMENT"));
        assertTrue(ui.contains("NOT ACTIVE"));
        assertTrue(log.contains("getNoBackupFilesDir"));
        assertTrue(log.contains("setDefaultUncaughtExceptionHandler"));
        assertTrue(log.contains("getHistoricalProcessExitReasons"));
        assertFalse(manifest.contains("android.permission.READ_LOGS"));
        assertFalse(log.contains("https://"));
        assertFalse(log.contains("getMessage()"));
    }
}
