package au.com.t1xperts.mailxperts;

import org.junit.Test;
import static org.junit.Assert.*;

public class DiagnosticSanitizerTest {
    @Test public void exceptionMessagesAndEmailContentsNeverAppear() {
        Exception failure = new IllegalStateException("alice@example.com token=very-secret patient record");
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("au.com.t1xperts.mailxperts.ComposeActivity", "onCreate", "ComposeActivity.java", 65),
                new StackTraceElement("example.secret.patient.Email", "patientName", "Email.java", 10)
        });
        String sanitized = DiagnosticSanitizer.exceptionClass(failure) + DiagnosticSanitizer.appFrames(failure);
        assertTrue(sanitized.contains("ComposeActivity#onCreate:65"));
        assertFalse(sanitized.contains("alice@example.com"));
        assertFalse(sanitized.contains("very-secret"));
        assertFalse(sanitized.contains("patient"));
    }

    @Test public void freeTextEventCodesAreDiscarded() {
        assertEquals("OTHER", DiagnosticSanitizer.code("hello@example.com"));
        assertEquals("OTHER", DiagnosticSanitizer.code("REPLY tapped with subject"));
        assertEquals("REPLY_TAPPED", DiagnosticSanitizer.code("REPLY_TAPPED"));
    }
}
