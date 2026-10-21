package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AiPolicyTest {
    @Test public void onlyP0AiTasksAreAllowed() {
        assertTrue(AiPolicy.isAllowedTask("summarize"));
        assertTrue(AiPolicy.isAllowedTask("draft_reply"));
        assertTrue(AiPolicy.isAllowedTask("ask"));
        assertTrue(AiPolicy.isAllowedTask("search_plan"));
        assertTrue(AiPolicy.isAllowedTask("priority"));
        assertTrue(AiPolicy.isAllowedTask("extract_actions"));
        assertTrue(AiPolicy.isAllowedTask("rewrite"));
        assertFalse(AiPolicy.isAllowedTask("send"));
        assertFalse(AiPolicy.isAllowedTask("delete"));
    }

    @Test public void gatewayMustBeHttpsAndMustNotContainCredentials() {
        assertEquals("https://ai.t1xperts.example", AiPolicy.normaliseGateway("https://ai.t1xperts.example/"));
        reject("http://ai.t1xperts.example");
        reject("https://user:secret@ai.t1xperts.example");
        reject("not-a-url");
        reject("https://ai.t1xperts.example?token=sensitive");
        reject("https://ai.t1xperts.example/#fragment");
        reject("https://ai.t1xperts.example/../internal");
        reject("https://ai.t1xperts.example/%2e%2e/internal");
        reject("https://ai.t1xperts.example/%2fprivate");
        assertEquals("https://ai.t1xperts.example/api",
                AiPolicy.normaliseGateway("  https://ai.t1xperts.example/api///  "));
    }

    @Test public void payloadClampIsDeterministic() {
        assertEquals("abc", AiPolicy.clamp("abcdef", 3));
        assertEquals("", AiPolicy.clamp(null, 10));
    }

    private static void reject(String value) {
        try {
            AiPolicy.normaliseGateway(value);
            fail("Expected gateway to be rejected: " + value);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
