package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailHtmlPolicyTest {
    @Test public void preservesSenderInlineFormattingAndUsesNeutralCanvas() {
        String sender = "<p style=\"color:#123456;background:#ffeeaa\">Hello</p>";
        String wrapped = EmailHtmlPolicy.wrapForDisplay(sender);

        assertTrue(wrapped.contains(sender));
        assertTrue(wrapped.contains("content='only light'"));
        assertTrue(wrapped.contains("background:#ffffff"));
        assertFalse(wrapped.toLowerCase().contains("invert("));
        assertFalse(wrapped.toLowerCase().contains("filter:"));
    }
}
