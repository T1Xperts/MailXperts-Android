package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExternalLinkPolicyTest {
    @Test public void permitsOnlyExpectedExternalSchemes() {
        assertTrue(ExternalLinkPolicy.isAllowedScheme("https"));
        assertTrue(ExternalLinkPolicy.isAllowedScheme("HTTP"));
        assertTrue(ExternalLinkPolicy.isAllowedScheme("mailto"));
        assertTrue(ExternalLinkPolicy.isAllowedScheme("tel"));
        assertTrue(ExternalLinkPolicy.isAllowedScheme("sms"));
        assertTrue(ExternalLinkPolicy.isAllowedScheme("geo"));

        assertFalse(ExternalLinkPolicy.isAllowedScheme("javascript"));
        assertFalse(ExternalLinkPolicy.isAllowedScheme("data"));
        assertFalse(ExternalLinkPolicy.isAllowedScheme("file"));
        assertFalse(ExternalLinkPolicy.isAllowedScheme("content"));
        assertFalse(ExternalLinkPolicy.isAllowedScheme(""));
        assertFalse(ExternalLinkPolicy.isAllowedScheme(null));
    }
}
