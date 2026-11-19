package au.com.t1xperts.mailxperts;

import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public final class ReplyPayloadStoreTest {
    @Test public void largeHtmlIsStoredPrivatelyAndRestoredWithoutIntentExtras() throws Exception {
        File dir = Files.createTempDirectory("mx-reply-test-").toFile();
        try {
            String html = "<blockquote>" + "<p>বাংলা ✅ Café </p>".repeat(20000) + "</blockquote>";
            String token = ReplyPayloadStore.saveIn(dir, html);
            assertEquals(36, token.length());
            assertEquals(html, ReplyPayloadStore.loadFrom(dir, token));
            assertTrue(new File(dir, token + ".html").exists());
        } finally { cleanup(dir); }
    }

    @Test public void rejectsPathTraversalTokens() throws Exception {
        File dir = Files.createTempDirectory("mx-reply-test-").toFile();
        try {
            try {
                ReplyPayloadStore.loadFrom(dir, "../credentials");
                fail("Unsafe quote reference accepted");
            } catch (IOException expected) {}
        } finally { cleanup(dir); }
    }

    @Test public void refusesOversizedContentWithoutWritingFile() throws Exception {
        File dir = Files.createTempDirectory("mx-reply-test-").toFile();
        try {
            try {
                ReplyPayloadStore.saveIn(dir, "a".repeat(ReplyPayloadStore.MAX_QUOTE_BYTES + 1));
                fail("Oversized quote was accepted");
            } catch (IOException expected) {}
            assertEquals(0, dir.listFiles().length);
        } finally { cleanup(dir); }
    }

    @Test public void rejectsExpiredQuote() throws Exception {
        File dir = Files.createTempDirectory("mx-reply-test-").toFile();
        try {
            String token = ReplyPayloadStore.saveIn(dir, "<p>old</p>");
            File file = new File(dir, token + ".html");
            assertTrue(file.setLastModified(System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000));
            try { ReplyPayloadStore.loadFrom(dir, token); fail("Expired quote accepted"); }
            catch (IOException expected) {}
        } finally { cleanup(dir); }
    }

    private static void cleanup(File dir) {
        File[] children = dir.listFiles();
        if (children != null) for (File child : children) child.delete();
        dir.delete();
    }
}
