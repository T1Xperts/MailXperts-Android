package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReplyForwardFormatterTest {

    @Test public void replyAddsOriginalMessageMetadataAndBody() {
        String html = ReplyForwardFormatter.replyChain(
                "Alice <alice@example.com>", "Rafi <rafi@example.com>", "",
                "10 Sep 2026 2:30 pm", "Project update", "<p>Hello <b>Rafi</b></p>");
        assertTrue(html.contains("data-mailxperts-quoted=\"reply\""));
        assertTrue(html.contains("Original message"));
        assertTrue(html.contains("Alice &lt;alice@example.com&gt;"));
        assertTrue(html.contains("Rafi &lt;rafi@example.com&gt;"));
        assertTrue(html.contains("10 Sep 2026 2:30 pm"));
        assertTrue(html.contains("Project update"));
        assertTrue(html.contains("<p>Hello <b>Rafi</b></p>"));
    }

    @Test public void replyIncludesCcWhenPresent() {
        String html = ReplyForwardFormatter.replyChain(
                "a@example.com", "b@example.com", "c@example.com",
                "date", "subject", "<p>Body</p>");
        assertTrue(html.contains("<strong>Cc:</strong> c@example.com"));
    }

    @Test public void replyOmitsEmptyCc() {
        String html = ReplyForwardFormatter.replyChain(
                "a@example.com", "b@example.com", "  ",
                "date", "subject", "<p>Body</p>");
        assertFalse(html.contains("<strong>Cc:</strong>"));
    }

    @Test public void replyPreservesExistingNestedConversationChain() {
        String original = "<p>Newest message</p><blockquote><p>Older message</p>"
                + "<blockquote>Oldest message</blockquote></blockquote>";
        String html = ReplyForwardFormatter.replyChain(
                "a@example.com", "b@example.com", "", "date", "subject", original);
        assertTrue(html.contains(original));
        assertTrue(html.contains("Older message"));
        assertTrue(html.contains("Oldest message"));
    }

    @Test public void replyPreservesTablesLinksAndFormatting() {
        String original = "<table><tr><td><a href='https://example.com'><b>Value</b></a></td></tr></table>";
        String html = ReplyForwardFormatter.replyChain(
                "a@example.com", "b@example.com", "", "date", "subject", original);
        assertTrue(html.contains(original));
    }

    @Test public void replyEscapesUntrustedHeaderMetadata() {
        String html = ReplyForwardFormatter.replyChain(
                "<script>alert('x')</script>", "A&B <a@example.com>", "",
                "<date>", "A < B & C", "<p>Safe body path</p>");
        assertFalse(html.contains("<script>alert"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"));
        assertTrue(html.contains("A&amp;B &lt;a@example.com&gt;"));
        assertTrue(html.contains("A &lt; B &amp; C"));
    }

    @Test public void replyHandlesNullFieldsAndEmptyBody() {
        String html = ReplyForwardFormatter.replyChain(null, null, null, null, null, null);
        assertTrue(html.contains("Original message"));
        assertTrue(html.contains("(No message body)"));
    }

    @Test public void replyHandlesWhitespaceOnlyBody() {
        String html = ReplyForwardFormatter.replyChain("a", "b", "", "d", "s", "   \n  ");
        assertTrue(html.contains("(No message body)"));
    }

    @Test public void forwardAddsForwardedHeaderAndPreservesHtml() {
        String original = "<div><h2>Invoice</h2><p>Amount: <strong>$120</strong></p></div>";
        String html = ReplyForwardFormatter.forwardChain(
                "Accounts <accounts@example.com>", "Rafi <rafi@example.com>",
                "finance@example.com", "10 Sep 2026", "Invoice", original);
        assertTrue(html.contains("data-mailxperts-quoted=\"forward\""));
        assertTrue(html.contains("---------- Forwarded message ----------"));
        assertTrue(html.contains("Accounts &lt;accounts@example.com&gt;"));
        assertTrue(html.contains("finance@example.com"));
        assertTrue(html.contains(original));
    }

    @Test public void forwardPreservesExistingChainInsteadOfFlatteningIt() {
        String original = "<p>Current</p><blockquote><p>Previous</p></blockquote>";
        String html = ReplyForwardFormatter.forwardChain(
                "a@example.com", "b@example.com", "", "date", "subject", original);
        assertTrue(html.contains("<blockquote><p>Previous</p></blockquote>"));
        assertFalse(html.contains("&lt;blockquote&gt;"));
    }

    @Test public void forwardOmitsCcWhenMissing() {
        String html = ReplyForwardFormatter.forwardChain(
                "a@example.com", "b@example.com", null, "date", "subject", "<p>Body</p>");
        assertFalse(html.contains("<strong>Cc:</strong>"));
    }

    @Test public void forwardEscapesHeaderMetadata() {
        String html = ReplyForwardFormatter.forwardChain(
                "A <a@example.com>", "B&B <b@example.com>", "", "date", "<Subject>", "<p>Body</p>");
        assertTrue(html.contains("A &lt;a@example.com&gt;"));
        assertTrue(html.contains("B&amp;B &lt;b@example.com&gt;"));
        assertTrue(html.contains("&lt;Subject&gt;"));
    }

    @Test public void replySubjectAddsPrefixOnce() {
        assertEquals("Re: Hello", ReplyForwardFormatter.replySubject("Hello"));
        assertEquals("Re: Hello", ReplyForwardFormatter.replySubject("Re: Hello"));
        assertEquals("RE: Hello", ReplyForwardFormatter.replySubject("RE: Hello"));
    }

    @Test public void forwardSubjectAddsPrefixOnceAndAcceptsFw() {
        assertEquals("Fwd: Hello", ReplyForwardFormatter.forwardSubject("Hello"));
        assertEquals("Fwd: Hello", ReplyForwardFormatter.forwardSubject("Fwd: Hello"));
        assertEquals("FW: Hello", ReplyForwardFormatter.forwardSubject("FW: Hello"));
    }

    @Test public void subjectHelpersHandleNull() {
        assertEquals("Re: ", ReplyForwardFormatter.replySubject(null));
        assertEquals("Fwd: ", ReplyForwardFormatter.forwardSubject(null));
    }

    @Test public void bodyIsIncludedExactlyOnce() {
        String marker = "UNIQUE_BODY_MARKER_123";
        String html = ReplyForwardFormatter.replyChain("a", "b", "", "d", "s", "<p>" + marker + "</p>");
        assertEquals(html.indexOf(marker), html.lastIndexOf(marker));
    }

    @Test public void unicodeConversationContentIsPreserved() {
        String original = "<p>বাংলা লেখা ✅ Café 日本語</p>";
        String html = ReplyForwardFormatter.replyChain(
                "ব্যক্তি <a@example.com>", "Rafi <b@example.com>", "", "তারিখ", "বিষয়", original);
        assertTrue(html.contains("বাংলা লেখা ✅ Café 日本語"));
        assertTrue(html.contains("বিষয়"));
    }
}
