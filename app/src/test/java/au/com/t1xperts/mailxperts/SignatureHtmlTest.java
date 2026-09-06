package au.com.t1xperts.mailxperts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SignatureHtmlTest {
    @Test public void legacyEscapedSignatureBecomesFormattedHtmlAgain() {
        String escaped = "&lt;!-- signature --&gt;&lt;table role=&quot;presentation&quot;&gt;"
                + "&lt;tr&gt;&lt;td style=&quot;color:#4f8b57&quot;&gt;T1Xperts&lt;/td&gt;"
                + "&lt;/tr&gt;&lt;/table&gt;";

        String normalised = SignatureHtml.normaliseStored(escaped);

        assertTrue(normalised.contains("<table role=\"presentation\">"));
        assertTrue(normalised.contains("<td style=\"color:#4f8b57\">T1Xperts</td>"));
        assertFalse(normalised.contains("&lt;table"));
        assertFalse(normalised.contains("<!--"));
    }

    @Test public void doublyEscapedMarkupIsMigrated() {
        String normalised = SignatureHtml.normaliseStored(
                "&amp;lt;div&amp;gt;Customer Care&amp;lt;/div&amp;gt;");

        assertTrue(normalised.contains("<div>Customer Care</div>"));
    }

    @Test public void safeEmailSignatureFormattingAndImagesArePreserved() {
        String html = "<table style=\"color:#123456\"><tr><td>"
                + "<a href=\"mailto:care@t1xperts.com.au\">Email</a>"
                + "<img src=\"https://t1xperts.com.au/logo.png\" style=\"width:120px\">"
                + "</td></tr></table>";

        String sanitised = SignatureHtml.sanitise(html);

        assertTrue(sanitised.contains("<table style=\"color:#123456\">"));
        assertTrue(sanitised.contains("href=\"mailto:care@t1xperts.com.au\""));
        assertTrue(sanitised.contains("src=\"https://t1xperts.com.au/logo.png\""));
    }

    @Test public void activeContentHandlersAndUnsafeUrlsAreRemoved() {
        String html = "<script>alert(1)</script><svg onload=steal()></svg>"
                + "<a onclick=\"steal()\" href=javascript:steal()>Click</a>"
                + "<img onerror='steal()' src='data:text/html;base64,AAAA'>";

        String sanitised = SignatureHtml.sanitise(html).toLowerCase();

        assertFalse(sanitised.contains("<script"));
        assertFalse(sanitised.contains("<svg"));
        assertFalse(sanitised.contains("onclick"));
        assertFalse(sanitised.contains("onerror"));
        assertFalse(sanitised.contains("javascript:"));
        assertFalse(sanitised.contains("data:text/html"));
        assertTrue(sanitised.contains("href=\"\""));
    }

    @Test public void fullHtmlDocumentKeepsBodyOnly() {
        assertTrue(SignatureHtml.sanitise(
                "<!doctype html><html><head><title>x</title></head>"
                        + "<body><p>Signature</p></body></html>")
                .equals("<p>Signature</p>"));
    }

    @Test public void signatureSizeIsBounded() {
        assertTrue(SignatureHtml.isWithinLimit("short"));
        assertFalse(SignatureHtml.isWithinLimit(null));
        assertFalse(SignatureHtml.isWithinLimit(
                new String(new char[SignatureHtml.MAX_HTML_CHARACTERS + 1])));
    }
}
